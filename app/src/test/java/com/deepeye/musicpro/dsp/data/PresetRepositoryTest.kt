package com.deepeye.musicpro.dsp.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.deepeye.musicpro.dsp.model.DspParams
import com.google.gson.Gson
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PresetRepositoryTest {

    private lateinit var db: DspDatabase
    private lateinit var dao: DspPresetDao
    private lateinit var repository: PresetRepository
    private val gson = Gson()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, DspDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.dspPresetDao()
        repository = PresetRepository(dao, gson)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testSeedBuiltIns_insertsExactlySevenPresets() = runTest {
        repository.seedBuiltinPresets()
        val count = dao.getBuiltinCount()
        assertEquals(7, count)
    }

    @Test
    fun testSaveAndLoadPreset_preservesAllFields() = runTest {
        val originalParams = DspParams(
            enabled = true,
            pgcGain = -3f,
            eqEnabled = true,
            eqBands = floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f),
            bassBoostEnabled = true,
            bassBoostStrength = 450
        )

        val id = repository.savePreset("My Custom Preset", originalParams)
        assertTrue(id > 0)

        val loadedParams = repository.getPresetParams(id).first()
        assertNotNull(loadedParams)
        assertEquals(originalParams.enabled, loadedParams!!.enabled)
        assertEquals(originalParams.pgcGain, loadedParams.pgcGain, 0.001f)
        assertEquals(originalParams.eqEnabled, loadedParams.eqEnabled)
        assertArrayEquals(originalParams.eqBands, loadedParams.eqBands, 0.001f)
        assertEquals(originalParams.bassBoostEnabled, loadedParams.bassBoostEnabled)
        assertEquals(originalParams.bassBoostStrength, loadedParams.bassBoostStrength)
    }

    @Test
    fun testDeletePreset_removesUserPreset_keepsBuiltin() = runTest {
        // Seed built-in presets
        repository.seedBuiltinPresets()

        // Save a user preset
        val userParams = DspParams(enabled = true, pgcGain = -1f)
        val userId = repository.savePreset("User Preset", userParams)

        // Count total presets
        val allPresetsBefore = repository.getAllPresets().first()
        assertEquals(8, allPresetsBefore.size) // 7 builtin + 1 user

        // Try to delete a builtin preset ID (assuming ID 1 is a builtin preset)
        val builtinList = dao.getBuiltinPresetsList()
        val firstBuiltinId = builtinList.first().id
        repository.deletePreset(firstBuiltinId)

        // Verify builtin is NOT deleted because dao.deleteUserPreset specifies `is_builtin = 0`
        val allPresetsAfterBuiltinDelete = repository.getAllPresets().first()
        assertEquals(8, allPresetsAfterBuiltinDelete.size)

        // Delete user preset
        repository.deletePreset(userId)

        // Verify user preset is deleted
        val allPresetsAfterUserDelete = repository.getAllPresets().first()
        assertEquals(7, allPresetsAfterUserDelete.size)
        assertFalse(allPresetsAfterUserDelete.any { it.first == userId })
    }
}
