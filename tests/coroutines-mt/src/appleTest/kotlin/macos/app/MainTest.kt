package macos.app

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.normalizedCache
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.mpp.currentThreadId
import com.apollographql.apollo3.testing.runTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.test.Test

class MainTest {
  val json = """
    {
      "data": {
        "random": 42
      }
    }
  """.trimIndent()

  @Test
  fun coroutinesMtCanWork() = runTest {
    withContext(Dispatchers.Default) {
      println("Dispatchers.Default: ${currentThreadId()}")
      withContext(Dispatchers.Main.immediate) {
        println("Dispatchers.Main: ${currentThreadId()}")
        val server = MockServer()
        server.enqueue(json)
        val response = ApolloClient.Builder()
            .serverUrl(server.url())
            .build()
            .query(GetRandomQuery())
            .execute()
        check(response.dataAssertNoErrors.random == 42)
      }
    }
  }

  @Test
  fun freezingTheStoreIsPossible() = runTest {
    val server = MockServer()
    server.enqueue(json)
    val client = ApolloClient.Builder().serverUrl(server.url()).normalizedCache(MemoryCacheFactory()).build()
    withContext(Dispatchers.Default) {
      withContext(Dispatchers.Main.immediate) {
        val response = client.query(GetRandomQuery()).execute()
        check(response.dataAssertNoErrors.random == 42)
      }
    }
  }
}
