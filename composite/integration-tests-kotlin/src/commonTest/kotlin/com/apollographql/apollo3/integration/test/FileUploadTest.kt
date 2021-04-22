package com.apollographql.apollo3.integration.test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.CustomScalar
import com.apollographql.apollo3.api.Input
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.Upload
import com.apollographql.apollo3.api.UploadResponseAdapter
import com.apollographql.apollo3.api.internal.ApolloLogger
import com.apollographql.apollo3.cache.CacheHeaders
import com.apollographql.apollo3.integration.checkTestFixture
import com.apollographql.apollo3.integration.enqueue
import com.apollographql.apollo3.integration.mockserver.MockServer
import com.apollographql.apollo3.integration.mockserver.MockRecordedRequest
import com.apollographql.apollo3.integration.upload.MultipleUploadMutation
import com.apollographql.apollo3.integration.upload.NestedUploadMutation
import com.apollographql.apollo3.integration.upload.SingleUploadMutation
import com.apollographql.apollo3.integration.upload.SingleUploadTwiceMutation
import com.apollographql.apollo3.integration.upload.type.NestedObject
import com.apollographql.apollo3.testing.runWithMainLoop
import kotlinx.coroutines.flow.single
import okio.Buffer
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FileUploadTest {
  private val upload0: Upload = Upload.fromString("content_file0", "file0.txt", "plain/txt")
  private val upload1: Upload = Upload.fromString("content_file1", "file1.jpg", "image/jpeg")
  private val upload2: Upload = Upload.fromString("content_file2", "file2.png", "image/png")

  private val uploadSource: Upload = Upload.fromString("content_source", "source", "image/png")

  private val nestedObject0 = NestedObject(file = Input.Present(upload0), fileList = Input.Present(listOf(upload1, upload2)))
  private val nestedObject1 = NestedObject(file = Input.Present(upload1), fileList = Input.Present(listOf(upload0, upload2)))
  private val nestedObject2 = NestedObject(
      file = Input.Present(upload2),
      fileList = Input.Present(listOf(upload0, upload1)),
      recursiveNested = Input.Present(listOf(nestedObject0, nestedObject1))
  )

  private val mutationSingle = SingleUploadMutation(file = upload1)
  private val mutationSource = SingleUploadMutation(file = uploadSource)
  private val mutationTwice = SingleUploadTwiceMutation(file1 = upload1, file2 = upload2)
  private val mutationMultiple = MultipleUploadMutation(files = listOf(upload1, upload2))

  private val mutationNested = NestedUploadMutation(
      nested = Input.Present(nestedObject2),
      topFile = Input.Present(upload2),
      topFileList = Input.Present(listOf(upload1, upload0))
  )

  private val adapterCache = ResponseAdapterCache(mapOf(
      CustomScalar("Upload", "com.apollographql.apollo3.api.Upload") to UploadResponseAdapter
  ))

  private lateinit var mockServer: MockServer
  private lateinit var apolloClient: ApolloClient

  @BeforeTest
  fun setUp() {
    mockServer = MockServer()
    apolloClient = ApolloClient(mockServer.url())
  }

  @Test
  @Throws(Exception::class)
  fun testDefaultHttpCallWithUploadSingle() = runWithMainLoop {
    mockServer.enqueue("""
      {
        "data": null
      }
    """.trimIndent())
    apolloClient.mutate(mutationSingle).single()

    val request = mockServer.takeRequest()

    val parts = request.parts()

    assertEquals(parts.size, 3)

    assertOperationsPart(parts[0], "expectedOperationsPartBodySingle.json")
    assertMapPart(parts[1], "expectedMapPartBodySingle.json")
    assertFileContentPart(parts[2], "0", "file1.jpg", "image/jpeg")
  }
//
//  @Test
//  @Throws(Exception::class)
//  fun testDefaultHttpCallWithUploadSource() {
//    val requestAssertPredicate = Predicate<Request> { request ->
//      assertThat(request).isNotNull()
//      assertDefaultRequestHeaders(request!!, mutationSource)
//      assertThat(request.header(HttpCache.CACHE_KEY_HEADER)).isNull()
//      assertThat(request.header(HttpCache.CACHE_FETCH_STRATEGY_HEADER)).isNull()
//      assertThat(request.header(HttpCache.CACHE_EXPIRE_TIMEOUT_HEADER)).isNull()
//      assertThat(request.header(HttpCache.CACHE_EXPIRE_AFTER_READ_HEADER)).isNull()
//      assertThat(request.header(HttpCache.CACHE_PREFETCH_HEADER)).isNull()
//
//      assertThat(request.parts()[2].bytes.decodeToString()).isEqualTo("content_source")
//      true
//    }
//    val interceptor = ApolloServerInterceptor(serverUrl,
//        AssertHttpCallFactory(requestAssertPredicate), null, false,
//        adapterCache,
//        ApolloLogger(null))
//    interceptor.httpPostCall(mutationSource, CacheHeaders.NONE, RequestHeaders.NONE, true, false)
//  }
//
//  @Test
//  @Throws(Exception::class)
//  fun testDefaultHttpCallWithUploadTwice() {
//    val requestAssertPredicate = Predicate<Request> { request ->
//      assertThat(request).isNotNull()
//      assertDefaultRequestHeaders(request!!, mutationTwice)
//      assertThat(request.header(HttpCache.CACHE_KEY_HEADER)).isNull()
//      assertThat(request.header(HttpCache.CACHE_FETCH_STRATEGY_HEADER)).isNull()
//      assertThat(request.header(HttpCache.CACHE_EXPIRE_TIMEOUT_HEADER)).isNull()
//      assertThat(request.header(HttpCache.CACHE_EXPIRE_AFTER_READ_HEADER)).isNull()
//      assertThat(request.header(HttpCache.CACHE_PREFETCH_HEADER)).isNull()
//      assertRequestBodyTwice(request)
//      true
//    }
//    val interceptor = ApolloServerInterceptor(serverUrl,
//        AssertHttpCallFactory(requestAssertPredicate), null, false,
//        adapterCache,
//        ApolloLogger(null))
//    interceptor.httpPostCall(mutationTwice, CacheHeaders.NONE, RequestHeaders.NONE, true, false)
//  }
//
//  @Test
//  @Throws(Exception::class)
//  fun testDefaultHttpCallWithUploadMultiple() {
//    val requestAssertPredicate = Predicate<Request> { request ->
//      assertThat(request).isNotNull()
//      assertDefaultRequestHeaders(request!!, mutationMultiple)
//      assertThat(request.header(HttpCache.CACHE_KEY_HEADER)).isNull()
//      assertThat(request.header(HttpCache.CACHE_FETCH_STRATEGY_HEADER)).isNull()
//      assertThat(request.header(HttpCache.CACHE_EXPIRE_TIMEOUT_HEADER)).isNull()
//      assertThat(request.header(HttpCache.CACHE_EXPIRE_AFTER_READ_HEADER)).isNull()
//      assertThat(request.header(HttpCache.CACHE_PREFETCH_HEADER)).isNull()
//      assertRequestBodyMultiple(request)
//      true
//    }
//    val interceptor = ApolloServerInterceptor(serverUrl,
//        AssertHttpCallFactory(requestAssertPredicate), null, false,
//        adapterCache,
//        ApolloLogger(null))
//    interceptor.httpPostCall(mutationMultiple, CacheHeaders.NONE, RequestHeaders.NONE, true, false)
//  }
//
//  @Test
//  @Throws(Exception::class)
//  fun testDefaultHttpCallWithUploadNested() {
//    val requestAssertPredicate = Predicate<Request> { request ->
//      assertThat(request).isNotNull()
//      assertDefaultRequestHeaders(request!!, mutationNested)
//      assertRequestBodyNested(request)
//      true
//    }
//    val interceptor = ApolloServerInterceptor(serverUrl,
//        AssertHttpCallFactory(requestAssertPredicate), null, false,
//        adapterCache,
//        ApolloLogger(null))
//    interceptor.httpPostCall(mutationNested, CacheHeaders.NONE, RequestHeaders.NONE, true, false)
//  }
//
//  @Test
//  @Throws(Exception::class)
//  fun testAdditionalHeaders() {
//    val testHeader1 = "TEST_HEADER_1"
//    val testHeaderValue1 = "crappy_value"
//    val testHeader2 = "TEST_HEADER_2"
//    val testHeaderValue2 = "fantastic_value"
//    val testHeader3 = "TEST_HEADER_3"
//    val testHeaderValue3 = "awesome_value"
//    val requestAssertPredicate = Predicate<Request> { request ->
//      assertThat(request).isNotNull()
//      assertDefaultRequestHeaders(request!!, mutationSingle)
//      assertThat(request.header(HttpCache.CACHE_KEY_HEADER)).isNull()
//      assertThat(request.header(HttpCache.CACHE_FETCH_STRATEGY_HEADER)).isNull()
//      assertThat(request.header(HttpCache.CACHE_EXPIRE_TIMEOUT_HEADER)).isNull()
//      assertThat(request.header(HttpCache.CACHE_EXPIRE_AFTER_READ_HEADER)).isNull()
//      assertThat(request.header(HttpCache.CACHE_PREFETCH_HEADER)).isNull()
//      assertThat(request.header(testHeader1)).isEqualTo(testHeaderValue1)
//      assertThat(request.header(testHeader2)).isEqualTo(testHeaderValue2)
//      assertThat(request.header(testHeader3)).isEqualTo(testHeaderValue3)
//      assertRequestBodySingle(request)
//      true
//    }
//    val requestHeaders: RequestHeaders = RequestHeaders.builder()
//        .addHeader(testHeader1, testHeaderValue1)
//        .addHeader(testHeader2, testHeaderValue2)
//        .addHeader(testHeader3, testHeaderValue3)
//        .build()
//    val interceptor = ApolloServerInterceptor(serverUrl,
//        AssertHttpCallFactory(requestAssertPredicate), null, false,
//        adapterCache,
//        ApolloLogger(null))
//    interceptor.httpPostCall(mutationSingle, CacheHeaders.NONE, requestHeaders, true, false)
//  }

  private class Part(
      val contentLength: Long,
      val contentDisposition: String?,
      val contentType: String?,
      val bytes: ByteArray
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
      expectedContentType: String) {
    assertEquals(
        part.contentDisposition,
        "form-data; name=\"$expectedName\"; filename=\"$expectedFileName\""
    )
    assertEquals(part.contentType, expectedContentType)
  }
//
//  private fun assertRequestBodyTwice(request: Request) {
//    val parts = request.parts()
//
//    assertThat(parts.size).isEqualTo(4)
//
//    assertOperationsPart(parts[0], "expectedOperationsPartBodyTwice.json")
//    assertMapPart(parts[1], "expectedMapPartBodyTwice.json")
//    assertFileContentPart(parts[2], "0", "file1.jpg", "image/jpeg")
//    assertFileContentPart(parts[3], "1", "file2.png", "image/png")
//  }
//
//  private fun assertRequestBodyMultiple(request: Request) {
//    val parts = request.parts()
//
//    assertThat(parts.size).isEqualTo(4)
//
//    assertOperationsPart(parts[0], "expectedOperationsPartBodyMultiple.json")
//    assertMapPart(parts[1], "expectedMapPartBodyMultiple.json")
//    assertFileContentPart(parts[2], "0", "file1.jpg", "image/jpeg")
//    assertFileContentPart(parts[3], "1", "file2.png", "image/png")
//  }
//
//  private fun assertRequestBodyNested(request: Request) {
//    val parts = request.parts()
//
//    assertThat(parts.size).isEqualTo(14)
//
//    assertOperationsPart(parts[0], "expectedOperationsPartBodyNested.json")
//    assertMapPart(parts[1], "expectedMapPartBodyNested.json")
//  }
//
//  private class AssertHttpCallFactory(val predicate: Predicate<Request>) : Call.Factory {
//    override fun newCall(request: Request): Call {
//      if (!predicate.apply(request)) {
//        Assert.fail("Assertion failed")
//      }
//      return NoOpCall()
//    }
//  }
//
//  private class NoOpCall : Call {
//    override fun request(): Request {
//      throw UnsupportedOperationException()
//    }
//
//    override fun execute(): Response {
//      throw UnsupportedOperationException()
//    }
//
//    override fun enqueue(responseCallback: Callback) {}
//    override fun cancel() {}
//    override fun isExecuted(): Boolean {
//      return false
//    }
//
//    override fun isCanceled(): Boolean {
//      return false
//    }
//
//    override fun clone(): Call {
//      throw UnsupportedOperationException()
//    }
//
//    override fun timeout(): Timeout {
//      throw UnsupportedOperationException()
//    }
//  }
}