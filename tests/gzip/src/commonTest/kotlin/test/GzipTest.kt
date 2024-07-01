package test

import com.apollographql.apollo.ApolloClient
import com.apollographql.mockserver.MockResponse
import com.apollographql.mockserver.MockServer
import com.apollographql.apollo.testing.HostFileSystem
import com.apollographql.apollo.testing.internal.runTest
import com.apollographql.apollo.testing.testsPath
import gzip.GetStringQuery
import kotlinx.coroutines.CoroutineScope
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
    @Suppress("DEPRECATION")
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
