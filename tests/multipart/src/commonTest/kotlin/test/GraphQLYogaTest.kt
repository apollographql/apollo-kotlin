package test

import com.apollographql.apollo.ApolloClient
import com.apollographql.mockserver.MockResponse
import com.apollographql.mockserver.MockServer
import com.apollographql.apollo.testing.internal.runTest
import kotlinx.coroutines.CoroutineScope
import multipart.MyQuery
import okio.use
import kotlin.test.Test

class GraphQLYogaTest {
  /**
   * See https://github.com/apollographql/apollo-kotlin/issues/4596
   * GraphQL Yoga has fixed this in https://github.com/dotansimha/graphql-yoga/pull/2137 so this workaround is probably not needed
   */
  @Test
  fun emptyLastPartIsIgnored() = mockServerTest {
    mockServer.enqueue(
        MockResponse.Builder()
            .addHeader("Content-Type", "multipart/mixed; boundary=\"-\"")
            .body("---\r\n" +
                "Content-Type: application/json; charset=utf-8\r\n" +
                "Content-Length: 31\r\n" +
                "\r\n" +
                "{\"data\":{\"__typename\":\"Query\"}}\r\n" +
                "---\r\n" +
                "-----\r\n"
            )
            .build()
    )

    apolloClient.query(MyQuery()).execute()
  }
}

class MockServerTest(val mockServer: MockServer, val apolloClient: ApolloClient, val scope: CoroutineScope)

fun mockServerTest(
    clientBuilder: ApolloClient.Builder.() -> Unit = {},
    block: suspend MockServerTest.() -> Unit
) = runTest() {
  MockServer().use { mockServer ->
    ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .apply(clientBuilder)
        .build()
        .use {apolloClient ->
          MockServerTest(mockServer, apolloClient, this).block()
        }
  }
}
