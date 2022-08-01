package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Upload
import com.apollographql.apollo3.api.toUpload
import com.apollographql.apollo3.integration.upload.SingleUploadTwiceMutation
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.network.okHttpClient
import com.apollographql.apollo3.testing.internal.runTest
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JvmFileUploadTest {
  private val upload0: Upload = File("src/jvmTest/resources/file0.txt").toUpload("plain/txt")

  private val upload1: Upload = File("src/jvmTest/resources/file1.jpg").toUpload("image/jpeg")

  private val mutationTwice = SingleUploadTwiceMutation(upload0, upload1)


  private lateinit var mockServer: MockServer
  private lateinit var apolloClient: ApolloClient

  private suspend fun setUp() {
    mockServer = MockServer()

    // We only test the data that is sent to the server, we don't really mind the response
    mockServer.enqueue("""
      {
        "data": null
      }
    """.trimIndent())

    apolloClient = ApolloClient.Builder().serverUrl(mockServer.url()).build()
  }

  private suspend fun tearDown() {
    mockServer.stop()
  }

  @Test
  @Throws(Exception::class)
  fun twice() = runTest(before = { setUp() }, after = { tearDown() }) {
    apolloClient.mutation(mutationTwice).execute()

    val request = mockServer.takeRequest()
    val parts = request.parts()

    val expectedBodyLength = 1009
    assertEquals(expectedBodyLength, request.body.size)
    assertEquals(expectedBodyLength.toString(), request.headers["Content-Length"])
    assertEquals(4, parts.size)
    assertOperationsPart(parts[0], "expectedOperationsPartBodyTwice.json")
    assertMapPart(parts[1], "expectedMapPartBodyTwice.json")
    assertFileContentPart(parts[2], "0", "file0.txt", "plain/txt", File("src/jvmTest/resources/file0.txt"))
    assertFileContentPart(parts[3], "1", "file1.jpg", "image/jpeg", File("src/jvmTest/resources/file1.jpg"))
  }

  @Test
  @Throws(Exception::class)
  fun loggingBodyWithOkHttpDoesntConsumeTheFiles() = runTest(before = { setUp() }, after = { tearDown() }) {
    val fullLog = StringBuilder()
    apolloClient = ApolloClient.Builder()
        .okHttpClient(OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor { log ->
              fullLog.appendLine(log)
            }.setLevel(HttpLoggingInterceptor.Level.BODY))
            .build())
        .serverUrl(mockServer.url())
        .build()

    apolloClient.mutation(mutationTwice).execute()
    assertTrue(fullLog.toString().contains("(one-shot body omitted)"))
  }
}

private fun assertFileContentPart(
    part: Part,
    expectedName: String,
    expectedFileName: String,
    expectedContentType: String,
    expectedFileContents: File,
) {
  assertEquals(
      part.contentDisposition,
      "form-data; name=\"$expectedName\"; filename=\"$expectedFileName\""
  )
  assertEquals(part.contentType, expectedContentType)
  assertContentEquals(part.bytes, expectedFileContents.readBytes())
}
