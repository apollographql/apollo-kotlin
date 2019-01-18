package com.apollographql.apollo.internal.interceptor

import com.apollographql.apollo.api.GraphqlUpload
import com.apollographql.apollo.api.internal.Optional
import com.apollographql.apollo.cache.CacheHeaders
import com.apollographql.apollo.integration.interceptor.UploadQuery
import com.apollographql.apollo.internal.ApolloLogger
import com.apollographql.apollo.response.ScalarTypeAdapters
import com.google.common.base.Predicate
import com.google.common.truth.Truth.assertThat
import junit.framework.Assert.fail
import okhttp3.*
import okio.Buffer
import org.junit.Test
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException

fun createGraphqlUpload(fileName: String, content: String, mimeType: String): GraphqlUpload {
    val tempDir = System.getProperty("java.io.tmpdir")
    val filePath = tempDir + "/" + fileName
    val f = File(filePath)
    val bw = BufferedWriter(FileWriter(f))
    bw.write(content)
    bw.close()
    return object : GraphqlUpload() {
        init {
            file = f
            mimetype = mimeType
        }
    }
}

class ApolloServerUploadInterceptorTest {
    private val serverUrl = HttpUrl.parse("http://google.com")
    private val file = createGraphqlUpload("test1.txt", "content_testOne", "text/plain")
    private val query = UploadQuery.builder()
            .file(file)
            .build()

    @Test
    fun testDefaultHttpCall() {
        val requestAssertPredicate = Predicate<Request> { request ->
            assertThat(request!!.header(ApolloServerInterceptor.HEADER_CONTENT_TYPE)).isEqualTo("multipart/form-data; boundary=--graphql-multipart-upload-boundary-85763456--")
            assertRequestBody(request, """
----graphql-multipart-upload-boundary-85763456--
Content-Disposition: form-data; name="operations"
Content-Type: application/json; charset=utf-8
Content-Length: 129

{"query":"query UploadQuery(${'$'}file: Upload) {  uploadQuery(file: ${'$'}file)}","operationName":"UploadQuery","variables":{"file":null}}
----graphql-multipart-upload-boundary-85763456--
Content-Disposition: form-data; name="map"
Content-Type: application/json; charset=utf-8
Content-Length: 24

{"0":["variables.file"]}
----graphql-multipart-upload-boundary-85763456--
Content-Disposition: form-data; name="0"; filename="test1.txt"
Content-Type: text/plain
Content-Length: 15

content_testOne
----graphql-multipart-upload-boundary-85763456----
    """.trimIndent())
            true
        }

        val interceptor = ApolloServerInterceptor(serverUrl!!,
                AssertHttpCallFactory(requestAssertPredicate), null, false,
                ScalarTypeAdapters(emptyMap()),
                ApolloLogger(Optional.absent()), false)

        interceptor.httpCall(query, CacheHeaders.NONE)
    }

    private fun assertRequestBody(request: Request?, expectedRequestBody: String) {
//            assertThat(request!!.body()!!.contentType()).isEqualTo("multipart/form-data; boundary=--graphql-multipart-upload-boundary-85763456--");
        val bodyBuffer = Buffer()
        request!!.body()!!.writeTo(bodyBuffer)
        assertThat(bodyBuffer.readUtf8().trimIndent()).isEqualTo(expectedRequestBody)
    }

    private class AssertHttpCallFactory internal constructor(internal val predicate: Predicate<Request>) : Call.Factory {

        override fun newCall(request: Request): Call {
            if (!predicate.apply(request)) {
                fail("Assertion failed")
            }
            return NoOpCall()
        }
    }

    private class NoOpCall : Call {
        override fun request(): Request? {
            return null
        }

        @Throws(IOException::class)
        override fun execute(): Response? {
            return null
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
            return this
        }
    }
}
