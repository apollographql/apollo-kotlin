package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.api.ApolloCacheHeaders
import com.apollographql.apollo3.cache.normalized.api.CacheHeaders
import com.apollographql.apollo3.cache.normalized.api.MaxAgeCacheResolver
import com.apollographql.apollo3.cache.normalized.api.TypePolicyCacheKeyGenerator
import com.apollographql.apollo3.cache.normalized.api.normalize
import com.apollographql.apollo3.cache.normalized.apolloStore
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.apollographql.apollo3.cache.normalized.normalizedCache
import com.apollographql.apollo3.cache.normalized.sql.SqlNormalizedCacheFactory
import com.apollographql.apollo3.cache.normalized.storeReceiveDate
import com.apollographql.apollo3.exception.CacheMissException
import com.apollographql.apollo3.mpp.currentTimeMillis
import com.apollographql.apollo3.testing.runTest
import sqlite.GetUserQuery
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

class MaxAgeTest {
  @Test
  fun expiredQueryThrows() = runTest {
    val maxAge = 10
    val client = ApolloClient.Builder()
        .normalizedCache(
            SqlNormalizedCacheFactory(null, true),
            TypePolicyCacheKeyGenerator,
            MaxAgeCacheResolver(maxAge)
        )
        .storeReceiveDate(true)
        .serverUrl("unused")
        .build()
    val query = GetUserQuery()
    val data = GetUserQuery.Data(GetUserQuery.User("John", "john@doe.com"))

    val records = query.normalize(data, CustomScalarAdapters.Empty, TypePolicyCacheKeyGenerator).values

    client.apolloStore.accessCache {
      // store records in the past
      it.merge(records, cacheHeaders(currentTimeMillis()/1000 - 11))
    }

    try {
      client.query(GetUserQuery()).fetchPolicy(FetchPolicy.CacheOnly).execute()
      fail("An exception was expected")
    } catch (e: CacheMissException) {
      assertTrue(e.age != null)
    }

    client.apolloStore.accessCache {
      // update records to be in the present
      it.merge(records, cacheHeaders(currentTimeMillis()/1000))
    }

    val response = client.query(GetUserQuery()).fetchPolicy(FetchPolicy.CacheOnly).execute()
    assertTrue(response.data?.user?.name == "John")
  }

  private fun cacheHeaders(date: Long): CacheHeaders {
    return CacheHeaders.Builder().addHeader(ApolloCacheHeaders.DATE, date.toString()).build()
  }
}