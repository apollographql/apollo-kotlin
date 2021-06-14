package test.declarativecache

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.CacheResolver
import com.apollographql.apollo3.cache.normalized.MemoryCacheFactory
import com.apollographql.apollo3.interceptor.cache.withStore
import com.apollographql.apollo3.testing.runWithMainLoop
import declarativecache.GetPromoBookQuery
import kotlin.test.BeforeTest
import kotlin.test.Test

class DeclarativeCacheTest {
  private lateinit var apolloClient: ApolloClient
  private lateinit var store: ApolloStore

  @BeforeTest
  fun setUp() {
    store = ApolloStore(MemoryCacheFactory(), CacheResolver.ID)
    apolloClient = ApolloClient("https://com.example/unused").withStore(store)
  }


  @Test
  fun AtKeyIsWorking() = runWithMainLoop{
    val operation = GetPromoBookQuery()
    val data = GetPromoBookQuery.Data(book = GetPromoBookQuery.Data.Book(title = "Test", isbn = "42"))

    store.writeOperation(operation, data)

//    store.
  }
}