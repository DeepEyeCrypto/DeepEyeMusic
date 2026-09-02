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
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

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

