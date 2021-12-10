import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.store
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.rx2.rxSingle
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
        .rxSingle()
        .test()
        .awaitDone(1, TimeUnit.SECONDS)
        .assertComplete()
        .assertValue {
          it.data?.random == 42
        }
  }

  @Test
  fun errorsAreReceived() {
    mockServer.enqueue("bad response")

    apolloClient.query(GetRandomQuery())
        .rxSingle()
        .test()
        .awaitDone(1, TimeUnit.SECONDS)
        .assertError {
          it is ApolloException
        }
  }
}

