// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.di

import com.deepeye.musicpro.data.source.remote.youtube.resolver.WebViewBridgeResolver
import com.deepeye.musicpro.domain.resolver.SourceResolver
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class ResolverModule {

    @Binds
    @IntoSet
    abstract fun bindWebViewBridgeResolver(
        resolver: WebViewBridgeResolver
    ): SourceResolver

}
