package com.apollographql.ijplugin.normalizedcache

interface NormalizedCacheProvider<P> {
  fun provide(parameters: P): Result<NormalizedCache>
}
