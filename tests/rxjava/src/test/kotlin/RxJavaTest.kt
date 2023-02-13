import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.store
import com.apollographql.apollo3.exception.CacheMissException
import com.apollographql.apollo3.exception.JsonEncodingException
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.rx2.rxFlowable
import kotlinx.coroutines.runBlocking
import rxjava.GetRandomQuery
import java.util.concurrent.TimeUnit
import kotlin.test.BeforeTest
import kotlin.test.Test

class RxJavaTest {
  private lateinit var mockServer: MockServer
  private lateinit var apolloClient: ApolloClient

  @BeforeTest
  fun setUp() {
    val store = ApolloStore(MemoryCacheFactory())
    mockServer = MockServer()
    apolloClient = ApolloClient.Builder().serverUrl(runBlocking { mockServer.url() }).store(store).build()
  }

  private val response = """
    {
      "data": {
        "random": 42
      }
    }
  """.trimIndent()

  @Test
  fun querySucceeds() {
    mockServer.enqueue(response)

    apolloClient.query(GetRandomQuery())
        .rxFlowable()
        .test()
        .awaitDone(1, TimeUnit.SECONDS)
        .assertComplete()
        .assertValueAt(0) {
          it.exception is CacheMissException
        }
        .assertValueAt(1) {
          it.data?.random == 42
        }
  }

  @Test
  fun errorsAreReceived() {
    mockServer.enqueue("bad response")

    apolloClient.query(GetRandomQuery())
        .rxFlowable()
        .test()
        .awaitDone(1, TimeUnit.SECONDS)
        .assertComplete()
        .assertValueAt(0) {
          it.exception is CacheMissException
        }
        .assertValueAt(1) {
          it.exception is JsonEncodingException
        }
  }
}

