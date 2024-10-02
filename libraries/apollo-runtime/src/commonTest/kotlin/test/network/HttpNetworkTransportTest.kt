package test.network

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.testing.internal.runTest
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.enqueueString
import okio.use
import test.FooQuery
import kotlin.test.Test
import kotlin.test.assertEquals

class HttpNetworkTransportTest {
  @Test
  fun graphqlResponseJsonContentTypeMayHaveNon2xxHttpCode() = runTest {
    MockServer().use { mockServer ->
      ApolloClient.Builder()
          .serverUrl(mockServer.url())
          .build()
          .use { apolloClient ->
            mockServer.enqueueString(string = FooQuery.errorResponse, statusCode = 500, contentType = "application/graphql-response+json")
            val response = apolloClient.query(FooQuery()).execute()
            assertEquals("Oh no! Something went wrong :(", response.errors?.single()?.message)
          }
    }
  }
}