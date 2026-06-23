package com.deepeye.musicpro.updates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

@HiltViewModel
class ChangelogViewModel
@Inject
constructor(
    private val prefs: UpdatePrefsManager,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson,
) : ViewModel() {
    private val _state = MutableStateFlow(UpdateState())
    val state = _state.asStateFlow()

    private var hasCheckedThisSession = false

    fun checkForUpdate(currentVersionCode: Int) {
        if (hasCheckedThisSession) return
        hasCheckedThisSession = true

        viewModelScope.launch {
            try {
                val lastShown = prefs.getLastShownVersion()
                val shouldShow = currentVersionCode > lastShown
                android.util.Log.d(
                    "ChangelogDialogTest",
                    "currentVersionCode: $currentVersionCode, lastShown: $lastShown, shouldShow: $shouldShow",
                )

                if (shouldShow) {
                    val currentVersionName = com.deepeye.musicpro.BuildConfig.VERSION_NAME.substringBefore("-").trim()
                    
                    var dynamicEntries = fetchDynamicChangelog(currentVersionName)
                    
                    if (dynamicEntries == null || dynamicEntries.isEmpty()) {
                        dynamicEntries = AppChangelog.entries
                            .filter { it.versionCode <= currentVersionCode }
                            .sortedByDescending { it.versionCode }
                    }

                    _state.value =
                        UpdateState(
                            currentVersionCode = currentVersionCode,
                            lastShownVersionCode = lastShown,
                            shouldShowChangelog = true,
                            changelogEntries = dynamicEntries,
                        )
                }
            } catch (e: Exception) {
                android.util.Log.e("ChangelogDialogTest", "Error reading prefs", e)
            }
        }
    }

    private suspend fun fetchCommitsForCompare(compareUrl: String): List<String> {
        return try {
            val match = "compare/(.*)".toRegex().find(compareUrl)
            val comparePart = match?.groupValues?.get(1) ?: return emptyList()

            val apiUrl = "https://api.github.com/repos/DeepEyeCrypto/DeepEyeMusic/compare/$comparePart"
            val request = Request.Builder()
                .url(apiUrl)
                .header("User-Agent", "DeepEyeMusicPro-App")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) return emptyList()

            val bodyString = response.body?.string() ?: return emptyList()
            val jsonObj = gson.fromJson(bodyString, JsonObject::class.java)
            val commitsArray = jsonObj.getAsJsonArray("commits") ?: return emptyList()

            val commitMessages = mutableListOf<String>()
            for (i in 0 until commitsArray.size()) {
                val commitObj = commitsArray[i].asJsonObject
                val commitData = commitObj.getAsJsonObject("commit")
                val message = commitData?.getAsJsonPrimitive("message")?.asString ?: continue
                val firstLine = message.split("\n")[0].trim()
                if (firstLine.isNotBlank()) {
                    commitMessages.add(firstLine)
                }
            }
            commitMessages.reversed() // Reverse to show newest commits first if desired, or keep as is. Usually GitHub returns oldest first in compare. We want newest first? Actually keeping them chronological is fine.
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun fetchDynamicChangelog(currentVersionName: String): List<ChangelogEntry>? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.github.com/repos/DeepEyeCrypto/DeepEyeMusic/releases")
                .header("User-Agent", "DeepEyeMusicPro-App")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val bodyString = response.body?.string() ?: return@withContext null
            val jsonArray = gson.fromJson(bodyString, JsonArray::class.java)

            val dynamicEntries = mutableListOf<ChangelogEntry>()
            for (i in 0 until jsonArray.size()) {
                val obj = jsonArray[i].asJsonObject
                val tagName = obj.getAsJsonPrimitive("tag_name")?.asString?.removePrefix("v")?.trim() ?: continue
                
                if (isNewerVersion(tagName, currentVersionName)) continue

                val name = obj.getAsJsonPrimitive("name")?.asString ?: "Release $tagName"
                val body = obj.getAsJsonPrimitive("body")?.asString ?: ""
                val publishedAt = obj.getAsJsonPrimitive("published_at")?.asString ?: ""
                
                val dateStr = if (publishedAt.length >= 10) publishedAt.substring(0, 10) else publishedAt

                val items = body.split("\n")
                    .map { it.trim() }
                    .filter { it.startsWith("-") || it.startsWith("*") }
                    .map { it.drop(1).trim() }
                    .filter { !it.startsWith("*Full Changelog") && !it.startsWith("Full Changelog") }
                    .toMutableList()

                val compareLinkMatch = "https://github.com/DeepEyeCrypto/DeepEyeMusic/compare/[\\w.\\-]+...[\\w.\\-]+".toRegex().find(body)
                if (compareLinkMatch != null) {
                    val compareUrl = compareLinkMatch.value
                    val commits = fetchCommitsForCompare(compareUrl)
                    items.addAll(commits)
                }

                if (items.isEmpty() && body.isNotBlank() && !body.contains("Full Changelog")) {
                    items.add(body.take(200))
                }

                if (items.isNotEmpty()) {
                    dynamicEntries.add(
                        ChangelogEntry(
                            versionCode = 0,
                            versionName = tagName,
                            releaseDate = dateStr,
                            title = name,
                            items = items.filter { it.isNotBlank() },
                            highlight = dynamicEntries.isEmpty()
                        )
                    )
                }
            }
            if (dynamicEntries.isNotEmpty()) dynamicEntries else null
        } catch (e: Exception) {
            null
        }
    }

    private fun isNewerVersion(
        latest: String,
        current: String,
    ): Boolean {
        val l = latest.split(".").map { it.toIntOrNull() ?: 0 }
        val c = current.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(l.size, c.size)
        for (i in 0 until maxLen) {
            val li = l.getOrNull(i) ?: 0
            val ci = c.getOrNull(i) ?: 0
            if (li > ci) return true
            if (li < ci) return false
        }
        return false
    }

    fun onDismiss() {
        viewModelScope.launch {
            try {
                prefs.markVersionShown(_state.value.currentVersionCode)
            } catch (e: Exception) {
                android.util.Log.e("Changelog", "Error reading prefs", e)
            }
            _state.update { it.copy(shouldShowChangelog = false) }
        }
    }

    fun snooze() {
        _state.update { it.copy(shouldShowChangelog = false) }
    }
}
