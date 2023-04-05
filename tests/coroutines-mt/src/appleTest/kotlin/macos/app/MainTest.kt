package macos.app

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.normalizedCache
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mpp.currentThreadName
import com.apollographql.apollo3.testing.enqueueData
import com.apollographql.apollo3.testing.internal.runTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.test.Test

class MainTest {
  private val data = GetRandomQuery.Data {
    random = 42
  }

  @Test
  fun coroutinesMtCanWork() = runTest {
    println("Current thread name: ${currentThreadName()}")
    withContext(Dispatchers.Default) {
      println("Current thread name: ${currentThreadName()}")
      val server = MockServer()
      server.enqueueData(data)
      val response = ApolloClient.Builder()
          .serverUrl(server.url())
          .build()
          .query(GetRandomQuery())
          .execute()
      check(response.dataOrThrow().random == 42)
    }
  }

  @Test
  fun freezingTheStoreIsPossible() = runTest {
    val server = MockServer()
    server.enqueueData(data)
    val client = ApolloClient.Builder().serverUrl(server.url()).normalizedCache(MemoryCacheFactory()).build()
    withContext(Dispatchers.Default) {
      val response = client.query(GetRandomQuery()).execute()
      check(response.dataOrThrow().random == 42)
    }
  }
}
