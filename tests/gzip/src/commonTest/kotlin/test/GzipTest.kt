package test

import com.apollographql.apollo3.mockserver.MockResponse
import com.apollographql.apollo3.testing.HostFileSystem
import com.apollographql.apollo3.testing.mockServerTest
import com.apollographql.apollo3.testing.testsPath
import gzip.GetStringQuery
import okio.Path.Companion.toPath
import okio.buffer
import okio.use
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GzipTest {
  @Test
  fun gzipTest() = mockServerTest {
    // This contains a valid response encoded with gzip
    val byteString = HostFileSystem.openReadOnly(testsPath.toPath().resolve("gzip/lorem.txt.gz")).use {
      it.source().buffer().readByteString()
    }

    mockServer.enqueue(
        MockResponse.Builder()
            .addHeader("content-type", "application/text")
            .addHeader("content-encoding", "gzip")
            .body(byteString)
            .build()
    )
    val response = apolloClient.query(GetStringQuery()).execute()
    assertEquals(2225, response.data?.longString?.length)
    assertTrue(response.data?.longString?.startsWith("Lorem ipsum dolor sit amet") == true)
  }
}