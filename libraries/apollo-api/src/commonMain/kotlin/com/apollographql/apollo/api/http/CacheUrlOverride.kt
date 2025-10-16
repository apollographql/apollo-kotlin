package com.apollographql.apollo.api.http

import com.apollographql.apollo.api.ExecutionContext

/**
 * Specifies a cache url override for caching requests that would otherwise not be cached, such as POST requests
 *
 * Note: not every `HttpEngine` supports cache url override.
 */
class CacheUrlOverride(val url: String): ExecutionContext.Element {
  override val key: ExecutionContext.Key<*>
    get() = Key

  companion object Key: ExecutionContext.Key<CacheUrlOverride>
}