package test

import com.apollographql.apollo3.mockserver.MockResponse
import com.apollographql.apollo3.testing.mockServerTest
import multipart.MyQuery
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