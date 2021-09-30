package macos.app

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.cache.normalized.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.NormalizedCacheFactory
import com.apollographql.apollo3.cache.normalized.withNormalizedCache
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
          val response = ApolloClient(server.url()).query(GetRandomQuery())
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
      val client = ApolloClient(server.url()).freeze()
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
      val client = ApolloClient(server.url()).withNormalizedCache(MemoryCacheFactory())
      withContext(Dispatchers.Default) {
        withContext(Dispatchers.Main) {
          val response = client.query(GetRandomQuery())
          check(response.dataOrThrow.random == 42)
        }
      }
    }
  }
}
