package com.apollographql.apollo3.internal.interceptor

import com.apollographql.apollo3.Utils.checkTestFixture
import com.apollographql.apollo3.api.CustomScalar
import com.apollographql.apollo3.api.FileUpload
import com.apollographql.apollo3.api.Input
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.Upload
import com.apollographql.apollo3.api.cache.http.HttpCache
import com.apollographql.apollo3.api.internal.ApolloLogger
import com.apollographql.apollo3.api.UploadResponseAdapter
import com.apollographql.apollo3.cache.CacheHeaders
import com.apollographql.apollo3.integration.upload.MultipleUploadMutation
import com.apollographql.apollo3.integration.upload.NestedUploadMutation
import com.apollographql.apollo3.integration.upload.SingleUploadMutation
import com.apollographql.apollo3.integration.upload.SingleUploadTwiceMutation
import com.apollographql.apollo3.integration.upload.type.NestedObject
import com.apollographql.apollo3.request.RequestHeaders
import com.google.common.base.Predicate
import com.google.common.truth.Truth.assertThat
import junit.framework.Assert
import okhttp3.*
import okio.*
import okio.ByteString.Companion.encodeUtf8
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import java.io.File
import java.lang.UnsupportedOperationException
import java.nio.charset.StandardCharsets
import java.nio.file.Files

class ApolloServerInterceptorFileUploadTest {
  private val serverUrl = HttpUrl.parse("http://google.com")!!
  private val file0 = createFile("file0.txt", "content_file0")
  private val file1 = createFile("file1.jpg", "content_file1")
  private val file2 = createFile("file2.png", "content_file2")
  private val upload0 = FileUpload("plain/txt", file0)
  private val upload1 = FileUpload("image/jpeg", file1)
  private val upload2 = FileUpload("image/png", file2)
  private val uploadSource = object: Upload {
    val content = "content_source".encodeUtf8()
    override val contentType = "image/png"
    override val contentLength = content.size.toLong()
    override val fileName = "source"
    override fun writeTo(sink: BufferedSink) {
      sink.writeAll(Buffer().write(content))
    }
  }

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

  private lateinit var mutationMultiple: MultipleUploadMutation

  private val mutationNested = NestedUploadMutation(nested = Input.Present(nestedObject2), topFile = Input.Present(upload2), topFileList = Input.Present(listOf(upload1, upload0)))

  private val adapterCache = ResponseAdapterCache(mapOf(
      CustomScalar("Upload", "com.apollographql.apollo3.api.Upload") to UploadResponseAdapter
  ))

  
  private fun createFile(fileName: String, content: String): File {
    val tempDir = System.getProperty("java.io.tmpdir")
    val filePath = "$tempDir/$fileName"
    val f = File(filePath)
    try {
      val bw = Files.newBufferedWriter(f.toPath(), StandardCharsets.UTF_8)
      bw.write(content)
      bw.close()
    } catch (e: Exception) {
    }
    return f
  }

  @Before
  fun prepare() {
    val uploads = listOf(upload1, upload2)

    mutationMultiple = MultipleUploadMutation(files = uploads)
  }

  @Test
  @Throws(Exception::class)
  fun testDefaultHttpCallWithUploadSingle() {
    val requestAssertPredicate = Predicate<Request> { request ->
      assertThat(request).isNotNull()
      assertDefaultRequestHeaders(request!!, mutationSingle)
      assertThat(request.header(HttpCache.CACHE_KEY_HEADER)).isNull()
      assertThat(request.header(HttpCache.CACHE_FETCH_STRATEGY_HEADER)).isNull()
      assertThat(request.header(HttpCache.CACHE_EXPIRE_TIMEOUT_HEADER)).isNull()
      assertThat(request.header(HttpCache.CACHE_EXPIRE_AFTER_READ_HEADER)).isNull()
      assertThat(request.header(HttpCache.CACHE_PREFETCH_HEADER)).isNull()
      assertRequestBodySingle(request)
      true
    }
    val interceptor = ApolloServerInterceptor(serverUrl,
        AssertHttpCallFactory(requestAssertPredicate),
        null,
        false,
        adapterCache,
        ApolloLogger(null))
    interceptor.httpPostCall(mutationSingle, CacheHeaders.NONE, RequestHeaders.NONE, true, false)
  }

  @Test
  @Throws(Exception::class)
  fun testDefaultHttpCallWithUploadSource() {
    val requestAssertPredicate = Predicate<Request> { request ->
      assertThat(request).isNotNull()
      assertDefaultRequestHeaders(request!!, mutationSource)
      assertThat(request.header(HttpCache.CACHE_KEY_HEADER)).isNull()
      assertThat(request.header(HttpCache.CACHE_FETCH_STRATEGY_HEADER)).isNull()
      assertThat(request.header(HttpCache.CACHE_EXPIRE_TIMEOUT_HEADER)).isNull()
      assertThat(request.header(HttpCache.CACHE_EXPIRE_AFTER_READ_HEADER)).isNull()
      assertThat(request.header(HttpCache.CACHE_PREFETCH_HEADER)).isNull()

      assertThat(request.parts()[2].bytes.decodeToString()).isEqualTo("content_source")
      true
    }
    val interceptor = ApolloServerInterceptor(serverUrl,
        AssertHttpCallFactory(requestAssertPredicate), null, false,
        adapterCache,
        ApolloLogger(null))
    interceptor.httpPostCall(mutationSource, CacheHeaders.NONE, RequestHeaders.NONE, true, false)
  }

  @Test
  @Throws(Exception::class)
  fun testDefaultHttpCallWithUploadTwice() {
    val requestAssertPredicate = Predicate<Request> { request ->
      assertThat(request).isNotNull()
      assertDefaultRequestHeaders(request!!, mutationTwice)
      assertThat(request.header(HttpCache.CACHE_KEY_HEADER)).isNull()
      assertThat(request.header(HttpCache.CACHE_FETCH_STRATEGY_HEADER)).isNull()
      assertThat(request.header(HttpCache.CACHE_EXPIRE_TIMEOUT_HEADER)).isNull()
      assertThat(request.header(HttpCache.CACHE_EXPIRE_AFTER_READ_HEADER)).isNull()
      assertThat(request.header(HttpCache.CACHE_PREFETCH_HEADER)).isNull()
      assertRequestBodyTwice(request)
      true
    }
    val interceptor = ApolloServerInterceptor(serverUrl,
        AssertHttpCallFactory(requestAssertPredicate), null, false,
        adapterCache,
        ApolloLogger(null))
    interceptor.httpPostCall(mutationTwice, CacheHeaders.NONE, RequestHeaders.NONE, true, false)
  }

  @Test
  @Throws(Exception::class)
  fun testDefaultHttpCallWithUploadMultiple() {
    val requestAssertPredicate = Predicate<Request> { request ->
      assertThat(request).isNotNull()
      assertDefaultRequestHeaders(request!!, mutationMultiple)
      assertThat(request.header(HttpCache.CACHE_KEY_HEADER)).isNull()
      assertThat(request.header(HttpCache.CACHE_FETCH_STRATEGY_HEADER)).isNull()
      assertThat(request.header(HttpCache.CACHE_EXPIRE_TIMEOUT_HEADER)).isNull()
      assertThat(request.header(HttpCache.CACHE_EXPIRE_AFTER_READ_HEADER)).isNull()
      assertThat(request.header(HttpCache.CACHE_PREFETCH_HEADER)).isNull()
      assertRequestBodyMultiple(request)
      true
    }
    val interceptor = ApolloServerInterceptor(serverUrl,
        AssertHttpCallFactory(requestAssertPredicate), null, false,
        adapterCache,
        ApolloLogger(null))
    interceptor.httpPostCall(mutationMultiple, CacheHeaders.NONE, RequestHeaders.NONE, true, false)
  }

  @Test
  @Throws(Exception::class)
  fun testDefaultHttpCallWithUploadNested() {
    val requestAssertPredicate = Predicate<Request> { request ->
      assertThat(request).isNotNull()
      assertDefaultRequestHeaders(request!!, mutationNested)
      assertRequestBodyNested(request)
      true
    }
    val interceptor = ApolloServerInterceptor(serverUrl,
        AssertHttpCallFactory(requestAssertPredicate), null, false,
        adapterCache,
        ApolloLogger(null))
    interceptor.httpPostCall(mutationNested, CacheHeaders.NONE, RequestHeaders.NONE, true, false)
  }

  @Test
  @Throws(Exception::class)
  fun testAdditionalHeaders() {
    val testHeader1 = "TEST_HEADER_1"
    val testHeaderValue1 = "crappy_value"
    val testHeader2 = "TEST_HEADER_2"
    val testHeaderValue2 = "fantastic_value"
    val testHeader3 = "TEST_HEADER_3"
    val testHeaderValue3 = "awesome_value"
    val requestAssertPredicate = Predicate<Request> { request ->
      assertThat(request).isNotNull()
      assertDefaultRequestHeaders(request!!, mutationSingle)
      assertThat(request.header(HttpCache.CACHE_KEY_HEADER)).isNull()
      assertThat(request.header(HttpCache.CACHE_FETCH_STRATEGY_HEADER)).isNull()
      assertThat(request.header(HttpCache.CACHE_EXPIRE_TIMEOUT_HEADER)).isNull()
      assertThat(request.header(HttpCache.CACHE_EXPIRE_AFTER_READ_HEADER)).isNull()
      assertThat(request.header(HttpCache.CACHE_PREFETCH_HEADER)).isNull()
      assertThat(request.header(testHeader1)).isEqualTo(testHeaderValue1)
      assertThat(request.header(testHeader2)).isEqualTo(testHeaderValue2)
      assertThat(request.header(testHeader3)).isEqualTo(testHeaderValue3)
      assertRequestBodySingle(request)
      true
    }
    val requestHeaders: RequestHeaders = RequestHeaders.builder()
        .addHeader(testHeader1, testHeaderValue1)
        .addHeader(testHeader2, testHeaderValue2)
        .addHeader(testHeader3, testHeaderValue3)
        .build()
    val interceptor = ApolloServerInterceptor(serverUrl,
        AssertHttpCallFactory(requestAssertPredicate), null, false,
        adapterCache,
        ApolloLogger(null))
    interceptor.httpPostCall(mutationSingle, CacheHeaders.NONE, requestHeaders, true, false)
  }

  private fun assertDefaultRequestHeaders(request: Request, mutation: Operation<*>) {
    assertThat(request.url()).isEqualTo(serverUrl)
    assertThat(request.method()).isEqualTo("POST")
    assertThat(request.header(ApolloServerInterceptor.HEADER_ACCEPT_TYPE)).isEqualTo(ApolloServerInterceptor.JSON_CONTENT_TYPE)
    assertThat(request.header(ApolloServerInterceptor.HEADER_APOLLO_OPERATION_ID)).isEqualTo(mutation.operationId())
    assertThat(request.header(ApolloServerInterceptor.HEADER_APOLLO_OPERATION_NAME)).isEqualTo(mutation.name())
    assertThat(request.tag()).isEqualTo(mutation.operationId())
  }

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

  private fun Request.parts(): List<Part> {
    val buffer = Buffer()
    val body = body()!!

    assertThat(body).isInstanceOf(RequestBody::class.java)
    assertThat(body.contentType()!!.type()).isEqualTo("multipart")
    assertThat(body.contentType()!!.subtype()).isEqualTo("form-data")

    val boundary = body.contentType()!!.toString().replace(Regex(".*boundary=(.*)"), "$1")
    assertThat(boundary).isNotEmpty()

    body.writeTo(buffer)
    buffer.flush()
    return buffer.parts(boundary)
  }

  private fun assertRequestBodySingle(request: Request) {
    val parts = request.parts()

    assertThat(parts.size).isEqualTo(3)

    assertOperationsPart(parts[0], "expectedOperationsPartBodySingle.json")
    assertMapPart(parts[1], "expectedMapPartBodySingle.json")
    assertFileContentPart(parts[2], "0", "file1.jpg", "image/jpeg")
  }

  private fun assertOperationsPart(part: Part, expectedPath: String) {
    assertThat(part.contentDisposition).isEqualTo("form-data; name=\"operations\"")
    assertThat(part.contentType).isEqualTo("application/json")
    checkTestFixture(part.bytes.decodeToString(), "ApolloServerInterceptorFileUploadTest/$expectedPath")
  }

  private fun assertMapPart(part: Part, expectedPath: String) {
    assertThat(part.contentDisposition).isEqualTo("form-data; name=\"map\"")
    assertThat(part.contentType).isEqualTo("application/json")
    checkTestFixture(part.bytes.decodeToString(), "ApolloServerInterceptorFileUploadTest/$expectedPath")
  }

  private fun assertFileContentPart(
      part: Part,
      expectedName: String,
      expectedFileName: String,
      expectedMimeType: String) {
    assertThat(part.contentDisposition).isEqualTo("form-data; name=\"" + expectedName +
        "\"; filename=\"" + expectedFileName + "\"")
    assertThat(part.contentType).isEqualTo(MediaType.parse(expectedMimeType).toString())
  }

  private fun assertRequestBodyTwice(request: Request) {
    val parts = request.parts()

    assertThat(parts.size).isEqualTo(4)

    assertOperationsPart(parts[0], "expectedOperationsPartBodyTwice.json")
    assertMapPart(parts[1], "expectedMapPartBodyTwice.json")
    assertFileContentPart(parts[2], "0", "file1.jpg", "image/jpeg")
    assertFileContentPart(parts[3], "1", "file2.png", "image/png")
  }

  private fun assertRequestBodyMultiple(request: Request) {
    val parts = request.parts()

    assertThat(parts.size).isEqualTo(4)

    assertOperationsPart(parts[0], "expectedOperationsPartBodyMultiple.json")
    assertMapPart(parts[1], "expectedMapPartBodyMultiple.json")
    assertFileContentPart(parts[2], "0", "file1.jpg", "image/jpeg")
    assertFileContentPart(parts[3], "1", "file2.png", "image/png")
  }

  private fun assertRequestBodyNested(request: Request) {
    val parts = request.parts()

    assertThat(parts.size).isEqualTo(14)

    assertOperationsPart(parts[0], "expectedOperationsPartBodyNested.json")
    assertMapPart(parts[1], "expectedMapPartBodyNested.json")
  }

  private class AssertHttpCallFactory(val predicate: Predicate<Request>) : Call.Factory {
    override fun newCall(request: Request): Call {
      if (!predicate.apply(request)) {
        Assert.fail("Assertion failed")
      }
      return NoOpCall()
    }
  }

  private class NoOpCall : Call {
    override fun request(): Request {
      throw UnsupportedOperationException()
    }

    override fun execute(): Response {
      throw UnsupportedOperationException()
    }

    override fun enqueue(responseCallback: Callback) {}
    override fun cancel() {}
    override fun isExecuted(): Boolean {
      return false
    }

    override fun isCanceled(): Boolean {
      return false
    }

    override fun clone(): Call {
      throw UnsupportedOperationException()
    }

    override fun timeout(): Timeout {
      throw UnsupportedOperationException()
    }
  }
}
