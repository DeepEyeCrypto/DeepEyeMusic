// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.di

import com.deepeye.musicpro.data.orchestrator.FeedCacheRepository
import com.deepeye.musicpro.data.orchestrator.HomeFeedOrchestrator
import com.deepeye.musicpro.data.orchestrator.PersonalizationRanker
import com.deepeye.musicpro.data.orchestrator.SectionFetcher
import com.deepeye.musicpro.data.orchestrator.ThermalGovernor
import com.deepeye.musicpro.data.prefs.FeedFeatureFlags
import com.deepeye.musicpro.data.prefs.SettingsDataStore
import com.deepeye.musicpro.account.AccountSessionManager
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt bindings for the unified home feed orchestrator pipeline.
 *
 * Migration note: this module ADDS to the existing PersonalizationModule.
 * No existing bindings are removed or altered. When FeedFeatureFlags are all
 * disabled, the orchestrator becomes a no-op passthrough and the legacy
 * PersonalizationRepositoryImpl continues to serve the UI unchanged.
 */
@Module
@InstallIn(SingletonComponent::class)
object HomeFeedOrchestratorModule {

    @Provides
    @Singleton
    fun provideThermalGovernor(
        @ApplicationContext context: Context,
    ): ThermalGovernor = ThermalGovernor(context)

    @Provides
    @Singleton
    fun provideFeedFeatureFlags(
        settingsDataStore: SettingsDataStore,
    ): FeedFeatureFlags = FeedFeatureFlags(settingsDataStore)

    @Provides
    @Singleton
    fun provideFeedCacheRepository(): FeedCacheRepository = FeedCacheRepository()

    @Provides
    @Singleton
    fun providePersonalizationRanker(): PersonalizationRanker = PersonalizationRanker()

    @Provides
    @Singleton
    fun provideHomeFeedOrchestrator(
        @ApplicationContext context: Context,
        sectionFetchers: Map<String, SectionFetcher>,
        ranker: PersonalizationRanker,
        cache: FeedCacheRepository,
        accountSession: AccountSessionManager,
        thermalGovernor: ThermalGovernor,
        featureFlags: FeedFeatureFlags,
    ): HomeFeedOrchestrator = HomeFeedOrchestrator(
        context = context,
        sectionFetchers = sectionFetchers,
        ranker = ranker,
        cache = cache,
        accountSession = accountSession,
        thermalGovernor = thermalGovernor,
        featureFlags = featureFlags,
    )
}