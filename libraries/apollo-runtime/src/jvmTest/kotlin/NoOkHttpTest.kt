import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.HttpResponse
import com.apollographql.apollo.network.http.HttpEngine
import com.apollographql.apollo.testing.internal.runTest
import okio.Buffer
import okio.use
import test.FooMutation
import test.FooQuery
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NoOkHttpTest {
  @Test
  fun customHttpEngineExecutesQueriesAndMutationsWithoutOkHttp() = runTest {
    assertFailsWith<ClassNotFoundException> {
      Class.forName("okhttp3.OkHttpClient")
    }
    val requests = mutableListOf<String>()
    var closed = false
    val httpEngine = object : HttpEngine {
      override suspend fun execute(request: HttpRequest): HttpResponse {
        requests.add(Buffer().apply { request.body!!.writeTo(this) }.readUtf8())
        return HttpResponse.Builder(200)
            .body(Buffer().writeUtf8(FooQuery.successResponse))
            .build()
      }

      override fun close() {
        closed = true
      }
    }

    ApolloClient.Builder()
        .serverUrl("https://example.com/graphql")
        .httpEngine(httpEngine)
        .build()
        .use { client ->
          val queryResponse = client.query(FooQuery()).execute()
          assertNull(queryResponse.exception)
          assertEquals(42, queryResponse.data?.foo)

          val mutationResponse = client.mutation(FooMutation()).execute()
          assertNull(mutationResponse.exception)
          assertEquals(42, mutationResponse.data?.foo)
        }

    assertEquals(2, requests.size)
    assertTrue(requests[0].contains("query FooOperation"))
    assertTrue(requests[1].contains("mutation FooOperation"))
    assertTrue(closed)
  }
}
