package test.declarativecache

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.CacheResolver
import com.apollographql.apollo3.cache.normalized.MemoryCacheFactory
import com.apollographql.apollo3.interceptor.cache.withStore
import com.apollographql.apollo3.mockserver.MockServer
import kotlin.test.BeforeTest
import kotlin.test.Test

class DeclarativeCacheTest {
  private lateinit var mockServer: MockServer
  private lateinit var apolloClient: ApolloClient
  private lateinit var store: ApolloStore



  @BeforeTest
  fun setUp() {
    store = ApolloStore(MemoryCacheFactory(), DeclarativecacheCacheResolver)
    mockServer = MockServer()
    apolloClient = ApolloClient(mockServer.url()).withStore(store)
  }


  @Test
  fun AtKeyIsWorking() {
    mockServer.enqueue()
  }
}