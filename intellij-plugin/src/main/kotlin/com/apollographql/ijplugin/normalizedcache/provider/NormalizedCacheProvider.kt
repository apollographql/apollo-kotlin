package com.apollographql.ijplugin.normalizedcache.provider

import com.apollographql.ijplugin.normalizedcache.NormalizedCache

interface NormalizedCacheProvider<P> {
  fun provide(parameters: P): Result<NormalizedCache>
}
