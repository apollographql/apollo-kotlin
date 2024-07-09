import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.cache.normalized.ApolloStore
import com.apollographql.apollo.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo.cache.normalized.store
import com.apollographql.apollo.exception.CacheMissException
import com.apollographql.apollo.exception.JsonEncodingException
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.enqueueString
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.rx2.asFlowable
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
    mockServer.enqueueString(response)

    apolloClient.query(GetRandomQuery())
        .toFlow()
        .asFlowable()
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
    mockServer.enqueueString("bad response")

    apolloClient.query(GetRandomQuery())
        .toFlow()
        .asFlowable()
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

