package test

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.cache.normalized.ApolloStore
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.api.MemoryCache
import com.apollographql.apollo.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo.cache.normalized.api.Record
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.apollographql.apollo.cache.normalized.memoryCacheOnly
import com.apollographql.apollo.cache.normalized.sql.SqlNormalizedCache
import com.apollographql.apollo.cache.normalized.sql.SqlNormalizedCacheFactory
import com.apollographql.apollo.cache.normalized.store
import com.apollographql.apollo.exception.CacheMissException
import com.apollographql.apollo.integration.normalizer.HeroNameQuery
import com.apollographql.apollo.testing.QueueTestNetworkTransport
import com.apollographql.apollo.testing.enqueueTestResponse
import com.apollographql.apollo.testing.internal.runTest
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MemoryCacheOnlyTest {
  @Test
  fun memoryCacheOnlyDoesNotStoreInSqlCache() = runTest {
    val store = ApolloStore(MemoryCacheFactory().chain(SqlNormalizedCacheFactory())).also { it.clearAll() }
    val apolloClient = ApolloClient.Builder().networkTransport(QueueTestNetworkTransport()).store(store).build()
    val query = HeroNameQuery()
    apolloClient.enqueueTestResponse(query, HeroNameQuery.Data(HeroNameQuery.Hero("R2-D2")))
    apolloClient.query(query).memoryCacheOnly(true).execute()
    val dump: Map<KClass<*>, Map<String, Record>> = store.dump()
    assertEquals(2, dump[MemoryCache::class]!!.size)
    assertEquals(0, dump[SqlNormalizedCache::class]!!.size)
  }

  @Test
  fun memoryCacheOnlyDoesNotReadFromSqlCache() = runTest {
    val store = ApolloStore(MemoryCacheFactory().chain(SqlNormalizedCacheFactory())).also { it.clearAll() }
    val query = HeroNameQuery()
    store.writeOperation(query, HeroNameQuery.Data(HeroNameQuery.Hero("R2-D2")))

    val store2 = ApolloStore(MemoryCacheFactory().chain(SqlNormalizedCacheFactory()))
    val apolloClient = ApolloClient.Builder().serverUrl("unused").store(store2).build()
    // The record in is in the SQL cache, but we request not to access it
    assertIs<CacheMissException>(
        apolloClient.query(query).fetchPolicy(FetchPolicy.CacheOnly).memoryCacheOnly(true).execute().exception
    )
  }
}
