package com.apollographql.apollo3.cache.http

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import com.apollographql.apollo3.annotations.ApolloDeprecatedSince.Version.v3_2_3
import com.apollographql.apollo3.api.http.HttpResponse
import java.io.IOException

interface ApolloHttpCache {
  fun read(cacheKey: String): HttpResponse

  @Deprecated("Implementations should implement writeIncremental instead", ReplaceWith("writeIncremental(cacheKey, response)"))
  @ApolloDeprecatedSince(v3_2_3)
  fun write(response: HttpResponse, cacheKey: String)

  /**
   * Store the [response] with the given [cacheKey] into the cache.
   * A new [HttpResponse] is returned whose body, when read, will write the contents to the cache.
   * The response's body is not consumed nor closed.
   */
  fun writeIncremental(response: HttpResponse, cacheKey: String): HttpResponse {
    @Suppress("DEPRECATION")
    write(response, cacheKey)
    return response
  }

  @Throws(IOException::class)
  fun clearAll()

  @Throws(IOException::class)
  fun remove(cacheKey: String)
}
