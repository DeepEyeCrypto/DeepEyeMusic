package com.deepeye.musicpro.core.plugins

import android.content.Context
import android.util.Log
import com.deepeye.musicpro.extractor.bridge.IExtractorBridge
import dalvik.system.DexClassLoader
import java.io.File
import java.io.FileOutputStream

object PluginManager {
    private const val TAG = "PluginManager"
    private const val PLUGIN_FILE_NAME = "extractor-plugin.apk"
    private var cachedExtractor: IExtractorBridge? = null

    fun getExtractor(context: Context): IExtractorBridge {
        cachedExtractor?.let { return it }

        val pluginDir = context.getDir("plugins", Context.MODE_PRIVATE)
        val pluginFile = File(pluginDir, PLUGIN_FILE_NAME)

        // If plugin does not exist in local storage, copy from assets (fallback)
        if (!pluginFile.exists()) {
            copyAssetToFile(context, "plugins/$PLUGIN_FILE_NAME", pluginFile)
        }

        // If it still doesn't exist, we can't do anything
        if (!pluginFile.exists()) {
            throw IllegalStateException("Extractor plugin not found locally or in assets!")
        }

        // Load via DexClassLoader
        val optimizedDexOutputPath = context.getDir("outdex", Context.MODE_PRIVATE)
        val dexClassLoader = DexClassLoader(
            pluginFile.absolutePath,
            optimizedDexOutputPath.absolutePath,
            null,
            context.classLoader
        )

        try {
            val pluginClass = dexClassLoader.loadClass("com.deepeye.musicpro.extractor.plugin.NewPipeExtractorPlugin")
            val extractor = pluginClass.getDeclaredConstructor().newInstance() as IExtractorBridge
            cachedExtractor = extractor
            return extractor
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load extractor plugin via DexClassLoader", e)
            throw RuntimeException("Failed to load extractor plugin", e)
        }
    }

    private fun copyAssetToFile(context: Context, assetPath: String, destFile: File) {
        try {
            context.assets.open(assetPath).use { inputStream ->
                FileOutputStream(destFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            Log.d(TAG, "Successfully copied $assetPath to ${destFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy asset $assetPath", e)
        }
    }
}
