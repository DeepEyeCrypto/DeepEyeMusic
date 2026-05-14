package com.deepeye.musicpro.dsp.di

import android.content.Context
import androidx.room.Room
import com.deepeye.musicpro.dsp.data.DspDatabase
import com.deepeye.musicpro.dsp.data.DspPresetDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for V4A DSP engine dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object V4AModule {

    @Provides
    @Singleton
    fun provideDspDatabase(@ApplicationContext context: Context): DspDatabase {
        return Room.databaseBuilder(
            context,
            DspDatabase::class.java,
            "deepeye_dsp.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDspPresetDao(database: DspDatabase): DspPresetDao = database.dspPresetDao()
}
