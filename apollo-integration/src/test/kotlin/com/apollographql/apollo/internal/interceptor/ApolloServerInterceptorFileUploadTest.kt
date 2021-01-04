package com.apollographql.apollo.internal.interceptor

import com.apollographql.apollo.Utils.checkTestFixture
import com.apollographql.apollo.api.CustomScalarAdapter
import com.apollographql.apollo.api.FileUpload
import com.apollographql.apollo.api.Input
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.ScalarType
import com.apollographql.apollo.api.ScalarTypeAdapters
import com.apollographql.apollo.api.cache.http.HttpCache
import com.apollographql.apollo.api.internal.ApolloLogger
import com.apollographql.apollo.cache.CacheHeaders
import com.apollographql.apollo.integration.upload.MultipleUploadMutation
import com.apollographql.apollo.integration.upload.NestedUploadMutation
import com.apollographql.apollo.integration.upload.SingleUploadMutation
import com.apollographql.apollo.integration.upload.SingleUploadTwiceMutation
import com.apollographql.apollo.integration.upload.type.NestedObject
import com.apollographql.apollo.request.RequestHeaders
import com.google.common.base.Predicate
import com.google.common.truth.Truth
import junit.framework.Assert
import okhttp3.*
import okio.*
import okio.ByteString.Companion.encodeUtf8
import org.junit.Before
import org.junit.Test
import java.io.File
import java.lang.UnsupportedOperationException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.*

class ApolloServerInterceptorFileUploadTest {
  private val serverUrl = HttpUrl.parse("http://google.com")!!
  private val file0 = createFile("file0.txt", "content_file0")
  private val file1 = createFile("file1.jpg", "content_file1")
  private val file2 = createFile("file2.png", "content_file2")
  private val upload0 = FileUpload("plain/txt", file0)
  private val upload1 = FileUpload("image/jpeg", file1)
  private val upload2 = FileUpload("image/png", file2)
  private val uploadSource = object: FileUpload("image/png") {
    val content = "content_source".encodeUtf8()
    override fun contentLength() = content.size.toLong()
    override fun fileName() = "source"
    override fun writeTo(sink: BufferedSink) {
      sink.writeAll(Buffer().let { it.write(content) })
    }
  }

  private val nestedObject0 = NestedObject(file = Input.optional(upload0), fileList = Input.optional(listOf(upload1, upload2)))
  private val nestedObject1 = NestedObject(file = Input.optional(upload1), fileList = Input.optional(listOf(upload0, upload2)))
  private val nestedObject2 = NestedObject(
      file = Input.optional(upload2),
      fileList = Input.optional(listOf(upload0, upload1)),
      recursiveNested = Input.optional(listOf(nestedObject0, nestedObject1))
  )

  private val mutationSingle = SingleUploadMutation(file = upload1)

  private val mutationSource = SingleUploadMutation(file = uploadSource)

  private val mutationTwice = SingleUploadTwiceMutation(file1 = upload1, file2 = upload2)

  private lateinit var mutationMultiple: MultipleUploadMutation

  private val mutationNested = NestedUploadMutation(nested = Input.optional(nestedObject2), topFile = Input.optional(upload2), topFileList = Input.optional(listOf(upload1, upload0)))

  private fun createFile(fileName: String, content: String): String {
    val tempDir = System.getProperty("java.io.tmpdir")
    val filePath = "$tempDir/$fileName"
    val f = File(filePath)
    try {
      val bw = Files.newBufferedWriter(f.toPath(), StandardCharsets.UTF_8)
      bw.write(content)
      bw.close()
    } catch (e: Exception) {
    }
    return f.path
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
      Truth.assertThat(request).isNotNull()
      assertDefaultRequestHeaders(request!!, mutationSingle)
      Truth.assertThat(request.header(HttpCache.CACHE_KEY_HEADER)).isNull()
      Truth.assertThat(request.header(HttpCache.CACHE_FETCH_STRATEGY_HEADER)).isNull()
      Truth.assertThat(request.header(HttpCache.CACHE_EXPIRE_TIMEOUT_HEADER)).isNull()
      Truth.assertThat(request.header(HttpCache.CACHE_EXPIRE_AFTER_READ_HEADER)).isNull()
      Truth.assertThat(request.header(HttpCache.CACHE_PREFETCH_HEADER)).isNull()
      assertRequestBodySingle(request)
      true
    }
    val interceptor = ApolloServerInterceptor(serverUrl,
        AssertHttpCallFactory(requestAssertPredicate), null, false,
        ScalarTypeAdapters(emptyMap<ScalarType, CustomScalarAdapter<*>>()),
        ApolloLogger(null))
    interceptor.httpPostCall(mutationSingle, CacheHeaders.NONE, RequestHeaders.NONE, true, false)
  }

  @Test
  @Throws(Exception::class)
  fun testDefaultHttpCallWithUploadSource() {
    val requestAssertPredicate = Predicate<Request> { request ->
      Truth.assertThat(request).isNotNull()
      assertDefaultRequestHeaders(request!!, mutationSource)
      Truth.assertThat(request.header(HttpCache.CACHE_KEY_HEADER)).isNull()
      Truth.assertThat(request.header(HttpCache.CACHE_FETCH_STRATEGY_HEADER)).isNull()
      Truth.assertThat(request.header(HttpCache.CACHE_EXPIRE_TIMEOUT_HEADER)).isNull()
      Truth.assertThat(request.header(HttpCache.CACHE_EXPIRE_AFTER_READ_HEADER)).isNull()
      Truth.assertThat(request.header(HttpCache.CACHE_PREFETCH_HEADER)).isNull()

      val buffer = Buffer()
      (request.body() as MultipartBody).part(2).body().writeTo(buffer)
      Truth.assertThat(buffer.readUtf8()).isEqualTo("content_source")
      true
    }
    val interceptor = ApolloServerInterceptor(serverUrl,
        AssertHttpCallFactory(requestAssertPredicate), null, false,
        ScalarTypeAdapters(emptyMap<ScalarType, CustomScalarAdapter<*>>()),
        ApolloLogger(null))
    interceptor.httpPostCall(mutationSource, CacheHeaders.NONE, RequestHeaders.NONE, true, false)
  }

  @Test
  @Throws(Exception::class)
  fun testDefaultHttpCallWithUploadTwice() {
    val requestAssertPredicate = Predicate<Request> { request ->
      Truth.assertThat(request).isNotNull()
      assertDefaultRequestHeaders(request!!, mutationTwice)
      Truth.assertThat(request.header(HttpCache.CACHE_KEY_HEADER)).isNull()
      Truth.assertThat(request.header(HttpCache.CACHE_FETCH_STRATEGY_HEADER)).isNull()
      Truth.assertThat(request.header(HttpCache.CACHE_EXPIRE_TIMEOUT_HEADER)).isNull()
      Truth.assertThat(request.header(HttpCache.CACHE_EXPIRE_AFTER_READ_HEADER)).isNull()
      Truth.assertThat(request.header(HttpCache.CACHE_PREFETCH_HEADER)).isNull()
      assertRequestBodyTwice(request)
      true
    }
    val interceptor = ApolloServerInterceptor(serverUrl,
        AssertHttpCallFactory(requestAssertPredicate), null, false,
        ScalarTypeAdapters(emptyMap<ScalarType, CustomScalarAdapter<*>>()),
        ApolloLogger(null))
    interceptor.httpPostCall(mutationTwice, CacheHeaders.NONE, RequestHeaders.NONE, true, false)
  }

  @Test
  @Throws(Exception::class)
  fun testDefaultHttpCallWithUploadMultiple() {
    val requestAssertPredicate = Predicate<Request> { request ->
      Truth.assertThat(request).isNotNull()
      assertDefaultRequestHeaders(request!!, mutationMultiple)
      Truth.assertThat(request.header(HttpCache.CACHE_KEY_HEADER)).isNull()
      Truth.assertThat(request.header(HttpCache.CACHE_FETCH_STRATEGY_HEADER)).isNull()
      Truth.assertThat(request.header(HttpCache.CACHE_EXPIRE_TIMEOUT_HEADER)).isNull()
      Truth.assertThat(request.header(HttpCache.CACHE_EXPIRE_AFTER_READ_HEADER)).isNull()
      Truth.assertThat(request.header(HttpCache.CACHE_PREFETCH_HEADER)).isNull()
      assertRequestBodyMultiple(request)
      true
    }
    val interceptor = ApolloServerInterceptor(serverUrl,
        AssertHttpCallFactory(requestAssertPredicate), null, false,
        ScalarTypeAdapters(emptyMap<ScalarType, CustomScalarAdapter<*>>()),
        ApolloLogger(null))
    interceptor.httpPostCall(mutationMultiple, CacheHeaders.NONE, RequestHeaders.NONE, true, false)
  }

  @Test
  @Throws(Exception::class)
  fun testDefaultHttpCallWithUploadNested() {
    val requestAssertPredicate = Predicate<Request> { request ->
      Truth.assertThat(request).isNotNull()
      assertDefaultRequestHeaders(request!!, mutationNested)
      assertRequestBodyNested(request)
      true
    }
    val interceptor = ApolloServerInterceptor(serverUrl,
        AssertHttpCallFactory(requestAssertPredicate), null, false,
        ScalarTypeAdapters(emptyMap<ScalarType, CustomScalarAdapter<*>>()),
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
      Truth.assertThat(request).isNotNull()
      assertDefaultRequestHeaders(request!!, mutationSingle)
      Truth.assertThat(request.header(HttpCache.CACHE_KEY_HEADER)).isNull()
      Truth.assertThat(request.header(HttpCache.CACHE_FETCH_STRATEGY_HEADER)).isNull()
      Truth.assertThat(request.header(HttpCache.CACHE_EXPIRE_TIMEOUT_HEADER)).isNull()
      Truth.assertThat(request.header(HttpCache.CACHE_EXPIRE_AFTER_READ_HEADER)).isNull()
      Truth.assertThat(request.header(HttpCache.CACHE_PREFETCH_HEADER)).isNull()
      Truth.assertThat(request.header(testHeader1)).isEqualTo(testHeaderValue1)
      Truth.assertThat(request.header(testHeader2)).isEqualTo(testHeaderValue2)
      Truth.assertThat(request.header(testHeader3)).isEqualTo(testHeaderValue3)
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
        ScalarTypeAdapters(emptyMap<ScalarType, CustomScalarAdapter<*>>()),
        ApolloLogger(null))
    interceptor.httpPostCall(mutationSingle, CacheHeaders.NONE, requestHeaders, true, false)
  }

  private fun assertDefaultRequestHeaders(request: Request, mutation: Operation<*, *>) {
    Truth.assertThat(request.url()).isEqualTo(serverUrl)
    Truth.assertThat(request.method()).isEqualTo("POST")
    Truth.assertThat(request.header(ApolloServerInterceptor.HEADER_ACCEPT_TYPE)).isEqualTo(ApolloServerInterceptor.ACCEPT_TYPE)
    Truth.assertThat(request.header(ApolloServerInterceptor.HEADER_CONTENT_TYPE)).isEqualTo(ApolloServerInterceptor.CONTENT_TYPE)
    Truth.assertThat(request.header(ApolloServerInterceptor.HEADER_APOLLO_OPERATION_ID)).isEqualTo(mutation.operationId())
    Truth.assertThat(request.header(ApolloServerInterceptor.HEADER_APOLLO_OPERATION_NAME)).isEqualTo(mutation.name().name())
    Truth.assertThat(request.tag()).isEqualTo(mutation.operationId())
  }

  private fun assertRequestBodySingle(request: Request) {
    Truth.assertThat(request.body()).isInstanceOf(MultipartBody::class.java)
    val body = request.body() as MultipartBody
    Truth.assertThat(body.contentType()!!.type()).isEqualTo("multipart")
    Truth.assertThat(body.contentType()!!.subtype()).isEqualTo("form-data")
    Truth.assertThat(body.parts().size).isEqualTo(3)

    // Check
    val part0 = body.parts()[0]
    assertOperationsPart(part0, "expectedOperationsPartBodySingle.json")
    val part1 = body.parts()[1]
    assertMapPart(part1, "expectedMapPartBodySingle.json")
    val part2 = body.parts()[2]
    assertFileContentPart(part2, "0", "file1.jpg", "image/jpeg")
  }

  private fun assertRequestBodyTwice(request: Request) {
    Truth.assertThat(request.body()).isInstanceOf(MultipartBody::class.java)
    val body = request.body() as MultipartBody?
    Truth.assertThat(body!!.contentType()!!.type()).isEqualTo("multipart")
    Truth.assertThat(body.contentType()!!.subtype()).isEqualTo("form-data")
    Truth.assertThat(body.parts().size).isEqualTo(4)

    // Check
    val part0 = body.parts()[0]
    assertOperationsPart(part0, "expectedOperationsPartBodyTwice.json")
    val part1 = body.parts()[1]
    assertMapPart(part1, "expectedMapPartBodyTwice.json")
    val part2 = body.parts()[2]
    assertFileContentPart(part2, "0", "file1.jpg", "image/jpeg")
    val part3 = body.parts()[3]
    assertFileContentPart(part3, "1", "file2.png", "image/png")
  }

  private fun assertRequestBodyMultiple(request: Request) {
    Truth.assertThat(request!!.body()).isInstanceOf(MultipartBody::class.java)
    val body = request.body() as MultipartBody?
    Truth.assertThat(body!!.contentType()!!.type()).isEqualTo("multipart")
    Truth.assertThat(body.contentType()!!.subtype()).isEqualTo("form-data")
    Truth.assertThat(body.parts().size).isEqualTo(4)

    // Check
    val part0 = body.parts()[0]
    assertOperationsPart(part0, "expectedOperationsPartBodyMultiple.json")
    val part1 = body.parts()[1]
    assertMapPart(part1, "expectedMapPartBodyMultiple.json")
    val part2 = body.parts()[2]
    assertFileContentPart(part2, "0", "file1.jpg", "image/jpeg")
    val part3 = body.parts()[3]
    assertFileContentPart(part3, "1", "file2.png", "image/png")
  }

  private fun assertRequestBodyNested(request: Request) {
    Truth.assertThat(request!!.body()).isInstanceOf(MultipartBody::class.java)
    val body = request.body() as MultipartBody?
    Truth.assertThat(body!!.contentType()!!.type()).isEqualTo("multipart")
    Truth.assertThat(body.contentType()!!.subtype()).isEqualTo("form-data")
    Truth.assertThat(body.parts().size).isEqualTo(14)

    // Check
    val part0 = body.parts()[0]
    assertOperationsPart(part0, "expectedOperationsPartBodyNested.json")
    val part1 = body.parts()[1]
    assertMapPart(part1, "expectedMapPartBodyNested.json")
  }

  private fun assertOperationsPart(part: MultipartBody.Part, expectedPath: String) {
    Truth.assertThat(part.headers()!!["Content-Disposition"]).isEqualTo("form-data; name=\"operations\"")
    Truth.assertThat(part.body().contentType()).isEqualTo(ApolloServerInterceptor.MEDIA_TYPE)
    val bodyBuffer = Buffer()
    try {
      part.body().writeTo(bodyBuffer)
    } catch (e: Exception) {
      throw RuntimeException(e)
    }
    checkTestFixture(bodyBuffer.readUtf8(), "ApolloServerInterceptorFileUploadTest/$expectedPath")
  }

  private fun assertMapPart(part: MultipartBody.Part, expectedPath: String) {
    Truth.assertThat(part.headers()!!["Content-Disposition"]).isEqualTo("form-data; name=\"map\"")
    Truth.assertThat(part.body().contentType()).isEqualTo(ApolloServerInterceptor.MEDIA_TYPE)
    val bodyBuffer = Buffer()
    try {
      part.body().writeTo(bodyBuffer)
    } catch (e: Exception) {
      throw RuntimeException(e)
    }
    checkTestFixture(bodyBuffer.readUtf8(), "ApolloServerInterceptorFileUploadTest/$expectedPath")
  }

  private fun assertFileContentPart(part: MultipartBody.Part, expectedName: String, expectedFileName: String,
                                    expectedMimeType: String) {
    Truth.assertThat(part.headers()!!["Content-Disposition"]).isEqualTo("form-data; name=\"" + expectedName +
        "\"; filename=\"" + expectedFileName + "\"")
    Truth.assertThat(part.body().contentType()).isEqualTo(MediaType.parse(expectedMimeType))
  }

  private class AssertHttpCallFactory internal constructor(val predicate: Predicate<Request>) : Call.Factory {
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
