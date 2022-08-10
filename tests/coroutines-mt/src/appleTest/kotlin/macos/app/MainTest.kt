package macos.app

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.api.json.jsonReader
import com.apollographql.apollo3.api.json.writeAny
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.normalizedCache
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.mpp.currentThreadName
import com.apollographql.apollo3.testing.internal.runTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Buffer
import kotlin.reflect.AssociatedObjectKey
import kotlin.reflect.ExperimentalAssociatedObjects
import kotlin.reflect.KClass
import kotlin.reflect.findAssociatedObject
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
    println("Current thread name: ${currentThreadName()}")
    withContext(Dispatchers.Default) {
      println("Current thread name: ${currentThreadName()}")
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

  @Test
  fun freezingTheStoreIsPossible() = runTest {
    val server = MockServer()
    server.enqueue(json)
    val client = ApolloClient.Builder().serverUrl(server.url()).normalizedCache(MemoryCacheFactory()).build()
    withContext(Dispatchers.Default) {
      val response = client.query(GetRandomQuery()).execute()
      check(response.dataAssertNoErrors.random == 42)
    }
  }
}
