package test.issue4010

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.runTest
import issue4010.ListQuery
import kotlin.test.Test
import kotlin.test.assertEquals

class EmptyListOfNonNull {
  @OptIn(ApolloExperimental::class)
  @Test
  fun test() = runTest {
    val mockServer = MockServer()
    mockServer.enqueue("""
      {
        "data": {
          "list": []
        }
      }
    """.trimIndent())
    val apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .build()

    val response = apolloClient.query(ListQuery()).execute()

    assertEquals(response.data?.list, emptyList())
  }
}
