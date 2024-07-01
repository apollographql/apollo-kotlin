package test

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.HttpResponse
import com.apollographql.apollo.integration.normalizer.HeroNameQuery
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.enqueueError
import com.apollographql.mockserver.enqueueString
import com.apollographql.apollo.network.http.DefaultHttpEngine
import com.apollographql.apollo.network.http.HttpEngine
import com.apollographql.apollo.testing.internal.runTest
import org.junit.Test
import kotlin.test.assertEquals

internal class MyHttpEngine : HttpEngine {
  private val wrappedHttpEngine = DefaultHttpEngine()
  val values = mutableListOf<String>()

  override suspend fun execute(request: HttpRequest): HttpResponse {
    request.executionContext[MyExecutionContext]?.also {
      values.add(it.value)
    }
    return wrappedHttpEngine.execute(request)
  }

  override fun close() {
    wrappedHttpEngine.close()
  }
}

class ExecutionContextTest {
  @Test
  fun executionContextIsAvailableInHttpInterceptor() = runTest {

    MockServer().use { mockServer ->
      val myHttpEngine = MyHttpEngine()
      ApolloClient.Builder()
          .serverUrl(mockServer.url())
          .httpEngine(myHttpEngine)
          .build().use { apolloClient ->

            // we don't need a response
            mockServer.enqueueError(statusCode = 404)
            apolloClient.query(HeroNameQuery())
                .addExecutionContext(MyExecutionContext("value0"))
                .execute()

            mockServer.enqueueError(statusCode = 404)
            apolloClient.query(HeroNameQuery())
                .addExecutionContext(MyExecutionContext("value1"))
                .execute()


            assertEquals(2, myHttpEngine.values.size)
            assertEquals("value0", myHttpEngine.values[0])
            assertEquals("value1", myHttpEngine.values[1])
          }
    }
  }
}

class MyExecutionContext(val value: String) : ExecutionContext.Element {
  companion object Key : ExecutionContext.Key<MyExecutionContext>

  override val key: ExecutionContext.Key<MyExecutionContext>
    get() = Key
}
