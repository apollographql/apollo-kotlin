package macos.app

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.cache.normalized.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.normalizedCache
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.mpp.currentThreadId
import com.apollographql.apollo3.testing.runWithMainLoop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.native.concurrent.freeze
import kotlin.test.Test
import kotlin.test.assertFailsWith

class MainTest {
  val json = """
    {
      "data": {
        "random": 42
      }
    }
  """.trimIndent()

  @Test
  fun coroutinesMtCanWork() {
    runWithMainLoop {
      withContext(Dispatchers.Default) {
        println("Dispatchers.Default: ${currentThreadId()}")
        withContext(Dispatchers.Main) {
          println("Dispatchers.Main: ${currentThreadId()}")
          val server = MockServer()
          server.enqueue(json)
          val response = ApolloClient.Builder().serverUrl(server.url()).build().query(GetRandomQuery())
          check(response.dataOrThrow.random == 42)
        }
      }
    }
  }

  @Test
  fun startingAQueryFromNonMainThreadAsserts() {
    runWithMainLoop {
      val server = MockServer()
      server.enqueue(json)
      val client = ApolloClient.Builder().serverUrl(server.url()).build().freeze()
      withContext(Dispatchers.Default) {
        assertFailsWith(IllegalStateException::class) {
          client.query(GetRandomQuery())
        }
      }
    }
  }

  @Test
  fun freezingTheStoreIsPossible() {
    runWithMainLoop {
      val server = MockServer()
      server.enqueue(json)
      val client = ApolloClient.Builder().serverUrl(server.url()).normalizedCache(MemoryCacheFactory()).build()
      withContext(Dispatchers.Default) {
        withContext(Dispatchers.Main) {
          val response = client.query(GetRandomQuery())
          check(response.dataOrThrow.random == 42)
        }
      }
    }
  }
}
