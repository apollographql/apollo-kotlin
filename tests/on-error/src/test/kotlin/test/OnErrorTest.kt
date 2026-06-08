package test

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.OnError
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.enqueueString
import com.example.GetAQuery
import com.example.GetHQuery
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class OnErrorTest {
  @Test
  fun onErrorNullDoesNotPropagateErrors(): Unit = runBlocking {
    MockServer().use { mockServer ->
      ApolloClient.Builder()
          .serverUrl(mockServer.url())
          .build()
          .use { apolloClient ->
            mockServer.enqueueString(
                // language=JSON
                """
              {
                "errors": [{ "message": "oops", "path": ["a", "f"] }],
                "data": { "a": { "f": null, "g": "g" } }
              }
            """.trimIndent())
            val response = apolloClient.query(GetAQuery())
                .onError(OnError.NULL)
                .execute()
            assertNotNull(response.data?.a)
            assertNull(response.data?.a?.f)
            assertEquals("g", response.data?.a?.g)
          }
    }
  }

  @Test
  fun onErrorNullDoesNotPropagateErrorsInLists(): Unit = runBlocking {
    MockServer().use { mockServer ->
      ApolloClient.Builder()
          .serverUrl(mockServer.url())
          .build()
          .use { apolloClient ->
            mockServer.enqueueString(
                // language=JSON
                """
              {
                "errors": [{ "message": "oops", "path": ["a", "h", 1] }],
                "data": { "a": { "h": ["h", null]} }
              }
            """.trimIndent())
            val response = apolloClient.query(GetHQuery())
                .onError(OnError.NULL)
                .execute()
            assertNotNull(response.data?.a)
            assertEquals("h", response.data?.a?.h?.get(0))
            assertNull(response.data?.a?.h?.get(1))
            assertEquals("oops", response.errors?.get(0)?.message)
          }
    }
  }
}
