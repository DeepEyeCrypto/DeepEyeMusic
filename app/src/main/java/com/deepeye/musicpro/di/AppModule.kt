// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.di

import android.content.Context
import com.deepeye.musicpro.data.prefs.PersonalizationPreferenceStore
import com.deepeye.musicpro.data.prefs.PersonalizationPreferencesDataStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * App-level Hilt module providing application-scoped dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindPersonalizationPreferenceStore(
        impl: PersonalizationPreferencesDataStore,
    ): PersonalizationPreferenceStore

    companion object {
        @Provides
        @Singleton
        fun provideApplicationContext(
            @ApplicationContext context: Context,
        ): Context = context
    }
}
