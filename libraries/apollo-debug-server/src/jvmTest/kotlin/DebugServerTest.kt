import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.http.HttpBody
import com.apollographql.apollo.api.json.buildJsonString
import com.apollographql.apollo.api.json.writeObject
import com.apollographql.apollo.cache.normalized.api.CacheHeaders
import com.apollographql.apollo.cache.normalized.api.CacheKey
import com.apollographql.apollo.cache.normalized.api.NormalizedCache
import com.apollographql.apollo.cache.normalized.api.NormalizedCacheFactory
import com.apollographql.apollo.cache.normalized.api.Record
import com.apollographql.apollo.cache.normalized.normalizedCache
import com.apollographql.apollo.debugserver.ApolloDebugServer
import com.apollographql.apollo.network.http.DefaultHttpEngine
import com.apollographql.apollo.network.http.HttpEngine
import com.apollographql.apollo.network.http.post
import kotlinx.coroutines.runBlocking
import okio.Buffer
import okio.BufferedSink
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertTrue

class DebugServerTest {
  class TestCacheFactory : NormalizedCacheFactory() {
    override fun create(): NormalizedCache {
      return object : NormalizedCache() {
        override fun merge(record: Record, cacheHeaders: CacheHeaders): Set<String> {
          TODO("Not yet implemented")
        }

        override fun merge(records: Collection<Record>, cacheHeaders: CacheHeaders): Set<String> {
          TODO("Not yet implemented")
        }

        override fun clearAll() {
          TODO("Not yet implemented")
        }

        override fun remove(cacheKey: CacheKey, cascade: Boolean): Boolean {
          TODO("Not yet implemented")
        }

        override fun remove(pattern: String): Int {
          TODO("Not yet implemented")
        }

        override fun loadRecord(key: String, cacheHeaders: CacheHeaders): Record? {
          TODO("Not yet implemented")
        }

        override fun loadRecords(keys: Collection<String>, cacheHeaders: CacheHeaders): Collection<Record> {
          TODO("Not yet implemented")
        }

        override fun dump(): Map<KClass<*>, Map<String, Record>> {
          return mapOf(
              NormalizedCache::class to mapOf(
                  "key" to Record(
                      "key",
                      mapOf("field1" to "value1", "field2" to CacheKey("value2"))
                  )
              )
          )
        }
      }
    }
  }

  @Test
  fun integrationTest() {
    val apolloClient = ApolloClient.Builder()
        .serverUrl("https://...")
        .normalizedCache(TestCacheFactory())
        .build()

    ApolloDebugServer.registerApolloClient(apolloClient)

    val engine = DefaultHttpEngine()

    runBlocking {
      /**
       * TODO: improve those checks
       */
      execute(engine, GetApolloClients, null).apply {
        assertTrue(contains("\"recordCount\":1"))
      }
      execute(engine, GetNormalizedCache, mapOf(
          "apolloClientId" to "client",
          "normalizedCacheId" to "client:com.apollographql.apollo.cache.normalized.api.NormalizedCache"
      )).apply {
        assertTrue(contains("\"field1\":\"value1\""))
        assertTrue(contains("\"field2\":\"ApolloCacheReference{value2}\""))
      }
    }
  }

  private suspend fun execute(engine: HttpEngine, query: String, variables: Map<String, String>?): String {
    val postBody = buildJsonString {
      writeObject {
        name("query")
        value(query)
        if (variables != null) {
          name("variables")
          writeObject {
            variables.forEach {
              name(it.key)
              value(it.value)
            }
          }
        }
      }
    }.let {
      Buffer().writeUtf8(it).readByteString()
    }

    return engine.post("http://localhost:8081/graphql")
        .body(object : HttpBody {
          override val contentLength: Long
            get() = postBody.size.toLong()
          override val contentType: String
            get() = "application/json"

          override fun writeTo(bufferedSink: BufferedSink) {
            bufferedSink.write(postBody)
          }
        })
        .execute()
        .body!!
        .readUtf8()
  }

  private val GetApolloClients = """
    query GetApolloClients {
      apolloClients {
        id
        displayName
        normalizedCaches {
          id
          displayName
          recordCount
        }
      }
    }
  """.trimIndent()

  private val GetNormalizedCache = """
    query GetNormalizedCache(${'$'}apolloClientId: ID!, ${'$'}normalizedCacheId: ID!) {
      apolloClient(id: ${'$'}apolloClientId) {
        normalizedCache(id: ${'$'}normalizedCacheId) {
          displayName
          records {
            key
            sizeInBytes
            fields
          }
        }
      }
    }
  """.trimIndent()
}