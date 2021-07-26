package test

import checkTestFixture
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.api.Upload
import com.apollographql.apollo3.api.UploadAdapter
import com.apollographql.apollo3.integration.upload.MultipleUploadMutation
import com.apollographql.apollo3.integration.upload.NestedUploadMutation
import com.apollographql.apollo3.integration.upload.SingleUploadMutation
import com.apollographql.apollo3.integration.upload.SingleUploadTwiceMutation
import com.apollographql.apollo3.integration.upload.type.NestedObject
import com.apollographql.apollo3.mockserver.MockRecordedRequest
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.runWithMainLoop
import okio.Buffer
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FileUploadTest {
  private val upload0: Upload = Upload.fromString("content_file0", "file0.txt", "plain/txt")
  private val upload1: Upload = Upload.fromString("content_file1", "file1.jpg", "image/jpeg")
  private val upload2: Upload = Upload.fromString("content_file2", "file2.png", "image/png")

  private val nestedObject0 = NestedObject(file = Optional.Present(upload0), fileList = Optional.Present(listOf(upload1, upload2)))
  private val nestedObject1 = NestedObject(file = Optional.Present(upload1), fileList = Optional.Present(listOf(upload0, upload2)))
  private val nestedObject2 = NestedObject(
      file = Optional.Present(upload2),
      fileList = Optional.Present(listOf(upload0, upload1)),
      recursiveNested = Optional.Present(listOf(nestedObject0, nestedObject1))
  )

  private val mutationSingle = SingleUploadMutation(file = upload1)
  private val mutationTwice = SingleUploadTwiceMutation(file1 = upload1, file2 = upload2)
  private val mutationMultiple = MultipleUploadMutation(files = listOf(upload1, upload2))
  private val mutationNested = NestedUploadMutation(
      nested = nestedObject2,
      topFile = upload2,
      topFileList = listOf(upload1, upload0)
  )

  private val adapterCache = CustomScalarAdapters(
      mapOf(
          "Upload" to UploadAdapter
      )
  )

  private lateinit var mockServer: MockServer
  private lateinit var apolloClient: ApolloClient

  @BeforeTest
  fun setUp() {
    mockServer = MockServer()

    // We only test the data that is sent to the server, we don't really mind the response
    mockServer.enqueue("""
      {
        "data": null
      }
    """.trimIndent())

    apolloClient = ApolloClient(mockServer.url())
  }

  @Test
  @Throws(Exception::class)
  fun single() = runWithMainLoop {
    apolloClient.mutate(mutationSingle)

    val request = mockServer.takeRequest()
    val parts = request.parts()

    assertEquals(parts.size, 3)
    assertOperationsPart(parts[0], "expectedOperationsPartBodySingle.json")
    assertMapPart(parts[1], "expectedMapPartBodySingle.json")
    assertFileContentPart(parts[2], "0", "file1.jpg", "image/jpeg")
  }


  @Test
  @Throws(Exception::class)
  fun twice() = runWithMainLoop {
    apolloClient.mutate(mutationTwice)

    val request = mockServer.takeRequest()
    val parts = request.parts()

    assertEquals(parts.size, 4)
    assertOperationsPart(parts[0], "expectedOperationsPartBodyTwice.json")
    assertMapPart(parts[1], "expectedMapPartBodyTwice.json")
    assertFileContentPart(parts[2], "0", "file1.jpg", "image/jpeg")
    assertFileContentPart(parts[3], "1", "file2.png", "image/png")
  }

  @Test
  @Throws(Exception::class)
  fun multiple() = runWithMainLoop {
    apolloClient.mutate(mutationMultiple)

    val request = mockServer.takeRequest()
    val parts = request.parts()
    assertEquals(parts.size, 4)
    assertOperationsPart(parts[0], "expectedOperationsPartBodyMultiple.json")
    assertMapPart(parts[1], "expectedMapPartBodyMultiple.json")
    assertFileContentPart(parts[2], "0", "file1.jpg", "image/jpeg")
    assertFileContentPart(parts[3], "1", "file2.png", "image/png")
  }


  @Test
  @Throws(Exception::class)
  fun nested() = runWithMainLoop {
    apolloClient.mutate(mutationNested)

    val request = mockServer.takeRequest()
    val parts = request.parts()
    assertEquals(parts.size, 14)
    assertMapPart(parts[1], "expectedMapPartBodyNested.json")
    assertOperationsPart(parts[0], "expectedOperationsPartBodyNested.json")
  }

  private class Part(
      val contentLength: Long,
      val contentDisposition: String?,
      val contentType: String?,
      val bytes: ByteArray,
  )

  private fun Buffer.parts(boundary: String): List<Part> {
    val parts = mutableListOf<Part>()
    var currentLength = -1L
    var currentDisposition: String? = null
    var currentType: String? = null

    while (true) {
      if (exhausted()) {
        error("no boundary found")
      }
      if (readUtf8Line() == "--$boundary") {
        break
      }
    }

    while (!exhausted()) {
      val line = readUtf8Line()!!
      when {
        line.startsWith("Content-Length: ") -> {
          currentLength = line.substring("Content-Length: ".length).toLong()
        }
        line.startsWith("Content-Disposition: ") -> {
          currentDisposition = line.substring("Content-Disposition: ".length)
        }
        line.startsWith("Content-Type: ") -> {
          currentType = line.substring("Content-Type: ".length)
        }
        line.isEmpty() -> {
          check(currentLength != -1L) {
            "We don't know how to read streamed multi part data (if that's even possible)"
          }
          parts.add(
              Part(
                  currentLength,
                  currentDisposition,
                  currentType,
                  buffer.readByteArray(currentLength)
              )
          )
          currentLength = -1
          currentDisposition = null
          currentType = null

          check(readByte() == '\r'.toByte())
          check(readByte() == '\n'.toByte())
          check(readUtf8("--$boundary".length.toLong()) == "--$boundary")
          when (val suffix = readUtf8(2)) {
            "--" -> {
              check(readByte() == '\r'.toByte())
              check(readByte() == '\n'.toByte())
              break
            }
            "\r\n" -> Unit
            else -> error("Unexpected suffix '$suffix'")
          }
        }
      }
    }
    return parts
  }

  private fun MockRecordedRequest.parts(): List<Part> {
    val regex = Regex("multipart/form-data;.*boundary=(.*)")
    val match = regex.matchEntire(headers["Content-Type"]!!)
    assertTrue(match != null)

    val boundary = match.groupValues[1]
    assertTrue(boundary.isNotBlank())

    return Buffer().apply { write(body) }.parts(boundary)
  }


  private fun assertOperationsPart(part: Part, fixtureName: String) {
    assertEquals(part.contentDisposition, "form-data; name=\"operations\"")
    assertEquals(part.contentType, "application/json")
    checkExpected(part.bytes.decodeToString(), fixtureName)
  }

  private fun assertMapPart(part: Part, fixtureName: String) {
    assertEquals(part.contentDisposition, "form-data; name=\"map\"")
    assertEquals(part.contentType, "application/json")
    checkExpected(part.bytes.decodeToString(), fixtureName)
  }

  private fun checkExpected(actualText: String, name: String) {
    checkTestFixture(actualText, "ApolloServerInterceptorFileUploadTest/$name")
  }

  private fun assertFileContentPart(
      part: Part,
      expectedName: String,
      expectedFileName: String,
      expectedContentType: String,
  ) {
    assertEquals(
        part.contentDisposition,
        "form-data; name=\"$expectedName\"; filename=\"$expectedFileName\""
    )
    assertEquals(part.contentType, expectedContentType)
  }
}