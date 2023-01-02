package test

import com.apollographql.apollo3.mockserver.MockResponse
import com.apollographql.apollo3.testing.mockServerTest
import multipart.MyQuery
import kotlin.test.Test

class MultipartTest {
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
