import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.apollographql.apollo3.cache.normalized.store
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.rx2.Rx2ApolloStore
import com.apollographql.apollo3.rx2.rxSingle
import com.apollographql.apollo3.rx2.toRx2ApolloStore
import kotlinx.coroutines.runBlocking
import rxjava.GetRandomQuery
import java.util.concurrent.TimeUnit
import kotlin.test.BeforeTest
import kotlin.test.Test

class RxJavaTest {
  private lateinit var mockServer: MockServer
  private lateinit var apolloClient: ApolloClient
  private lateinit var rx2ApolloStore: Rx2ApolloStore

  @BeforeTest
  fun setUp() {
    val store = ApolloStore(MemoryCacheFactory())
    mockServer = MockServer()
    apolloClient = ApolloClient.Builder().serverUrl(runBlocking { mockServer.url() }).store(store).build()
    rx2ApolloStore = store.toRx2ApolloStore()
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

  @Test
  fun writingToTheStoreWorks() {
    rx2ApolloStore.rxWriteOperation(GetRandomQuery(), GetRandomQuery.Data(random = 43)).blockingGet()
    apolloClient.query(GetRandomQuery())
        .fetchPolicy(FetchPolicy.CacheOnly)
        .rxSingle()
        .test()
        .awaitDone(1, TimeUnit.SECONDS)
        .assertValue {
          it.data?.random == 43
        }
  }
}

