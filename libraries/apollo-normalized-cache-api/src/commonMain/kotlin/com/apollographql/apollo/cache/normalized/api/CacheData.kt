package com.apollographql.apollo.cache.normalized.api

import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.annotations.ApolloInternal

/**
 * Data read from the cache that can be represented as a JSON map.
 *
 * @see [toData]
 */
@Deprecated("Use the new Normalized Cache at https://github.com/apollographql/apollo-kotlin-normalized-cache")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v5_0_0)
@ApolloInternal
interface CacheData {
  fun toMap(): Map<String, Any?>
}
