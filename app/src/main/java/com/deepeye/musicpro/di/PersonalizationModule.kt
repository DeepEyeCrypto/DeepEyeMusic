// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.di

import com.deepeye.musicpro.account.AccountDataSource
import com.deepeye.musicpro.account.AccountScopedCacheInvalidator
import com.deepeye.musicpro.account.SettingsAccountDataSourceImpl
import com.deepeye.musicpro.data.cache.AccountPersonalizationCache
import com.deepeye.musicpro.data.repository.PersonalizationRepositoryImpl
import com.deepeye.musicpro.domain.repository.PersonalizationRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Marks a [CoroutineScope] that lives for the whole process lifetime.
 *
 * Use this — not `viewModelScope` — for writes that MUST survive the caller's
 * lifecycle, such as persisting "onboarding completed" immediately before the
 * UI navigates away (which pops the back-stack entry and cancels the
 * ViewModel). Do not use it for work that should stop with its screen.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

/**
 * Hilt module for account-scoped personalization foundation components.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PersonalizationModule {

    @Binds
    @Singleton
    abstract fun bindAccountDataSource(
        impl: SettingsAccountDataSourceImpl
    ): AccountDataSource

    @Binds
    @Singleton
    abstract fun bindPersonalizationRepository(
        impl: PersonalizationRepositoryImpl
    ): PersonalizationRepository

    companion object {
        @Provides
        @Singleton
        fun provideCoroutineDispatcher(): CoroutineDispatcher = Dispatchers.IO

        /**
         * Process-lifetime scope for work that must outlive its caller.
         * `SupervisorJob` keeps one failed child from cancelling the rest.
         */
        @Provides
        @Singleton
        @ApplicationScope
        fun provideApplicationScope(
            dispatcher: CoroutineDispatcher,
        ): CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher)

        @Provides
        @Singleton
        fun provideCacheInvalidator(
            accountPersonalizationCache: AccountPersonalizationCache,
        ): AccountScopedCacheInvalidator =
            AccountScopedCacheInvalidator { accountKey ->
                accountPersonalizationCache.invalidateAccount(accountKey)
            }
    }
}

