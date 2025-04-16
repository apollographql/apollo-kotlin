package test

import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.enqueueString
import com.example.FooQuery
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MainTest {

  private fun runTest(
      response: String,
      configureClient: ApolloClient.Builder.() -> ApolloClient.Builder = { this },
      configureCall: ApolloCall<FooQuery.Data>.() -> ApolloCall<FooQuery.Data> = { this },
      assert: ApolloResponse<FooQuery.Data>.() -> Unit,
  ) = runBlocking {
    MockServer().use { mockServer ->
      mockServer.enqueueString(response)

      ApolloClient.Builder()
          .serverUrl(mockServer.url())
          .configureClient()
          .build()
          .use { apolloClient ->
            apolloClient.query(FooQuery())
                .configureCall()
                .execute()
                .apply(assert)
          }
    }

  }

  @Test
  fun extraDataField() {
    // language=json
    val response = """
      {
        "data": {
          "foo": 42,
          "user": {
            "name": "foo"
          },
          "extra": null
        }
      }
    """.trimIndent()

    // By default, no exception
    runTest(
        response,
    ) {
      assertNotNull(data)
      assertNull(exception)
    }

    // Setting the property on the client works
    runTest(
        response,
        configureClient = { ignoreUnknownKeys(false) },
    ) {
      assertNull(data)
      assertEquals("Unknown key 'extra' found at path: 'data.extra'", exception?.message)
    }

    // The property can also be set on the call
    runTest(
        response,
        configureCall = { ignoreUnknownKeys(false) },
    ) {
      assertNull(data)
      assertEquals("Unknown key 'extra' found at path: 'data.extra'", exception?.message)
    }
  }

  @Test
  fun extraNestedField() {
    // language=json
    val response = """
      {
        "data": {
          "foo": 42,
          "user": {
            "name": "foo",
            "extra": null
          }
        }
      }
    """.trimIndent()

    // The property can also be set on the call
    runTest(
        response,
        configureCall = { ignoreUnknownKeys(false) },
    ) {
      assertNull(data)
      assertEquals("Unknown key 'extra' found at path: 'data.user.extra'", exception?.message)
    }
  }

  @Test
  fun extraRootField() {
    // language=json
    val response = """
      {
        "data": {
          "foo": 42,
          "user": {
            "name": "foo"
          }
        },
        "extra": null
      }
    """.trimIndent()


    // The property can also be set on the call
    runTest(
        response,
        configureCall = { ignoreUnknownKeys(false) },
    ) {
      assertNull(data)
      assertEquals("Unknown key 'extra' found at path: 'extra'", exception?.message)
    }
  }

  @Test
  fun extraErrorField() {
    // language=json
    val response = """
      {
        "data": null,
        "errors": [
          { 
            "path": ["user", "name"],
            "message": "Cannot resolve user.name",
            "extra": null
          }
        ]
      }
    """.trimIndent()

    // The property can also be set on the call
    runTest(
        response,
        configureCall = { ignoreUnknownKeys(false) },
    ) {
      assertNull(data)
      assertEquals("Unknown key 'extra' found at path: 'errors.0.extra'", exception?.message)
    }
  }
}
