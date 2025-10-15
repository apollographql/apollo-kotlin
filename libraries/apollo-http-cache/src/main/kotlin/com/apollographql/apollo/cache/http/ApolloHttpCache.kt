package com.apollographql.apollo.cache.http

import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.api.http.HttpResponse
import java.io.IOException

@Deprecated("Use `OkHttpEngine(cachePostResponses = true)` instead. See https://go.apollo.dev/ak-http-cache")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v5_0_0)
interface ApolloHttpCache {
  fun read(cacheKey: String): HttpResponse

  /**
   * Store the [response] with the given [cacheKey] into the cache.
   * The response's body is not consumed nor closed.
   * @return a new [HttpResponse] whose body, when read, will write the contents to the cache.
   */
  fun write(response: HttpResponse, cacheKey: String): HttpResponse

  @Throws(IOException::class)
  fun clearAll()

  @Throws(IOException::class)
  fun remove(cacheKey: String)
}
