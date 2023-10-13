package com.apollographql.ijplugin.normalizedcache

interface NormalizedCacheProvider<P : NormalizedCacheProvider.Parameters> {
  interface Parameters

  fun provide(parameters: P): Result<NormalizedCache>
}
