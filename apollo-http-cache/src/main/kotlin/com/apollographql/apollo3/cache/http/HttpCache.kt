package com.apollographql.apollo3.cache.http

import com.apollographql.apollo3.api.http.HttpResponse

interface HttpCache {
  fun read(cacheKey: String): HttpResponse

  /**
   * Store the [response] with the given [cacheKey] into the cache.
   * Note: the response's body is not consumed nor closed.
   */
  fun write(response: HttpResponse, cacheKey: String)

  @Throws(IOException::class)
  fun clearAll()

  @Throws(IOException::class)
  fun remove(cacheKey: String)
}