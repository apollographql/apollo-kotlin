package com.apollographql.apollo.internal.interceptor

import com.apollographql.apollo.api.*
import com.apollographql.apollo.api.cache.http.HttpCachePolicy
import com.apollographql.apollo.api.internal.Optional
import com.apollographql.apollo.cache.CacheHeaders
import com.apollographql.apollo.internal.ApolloLogger
import com.apollographql.apollo.response.ScalarTypeAdapters
import com.google.common.truth.Truth.assertThat
import okhttp3.*
import okhttp3.Response
import okio.Buffer
import okio.BufferedSink
import okio.Okio
import org.junit.Test
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Paths

fun createFile(fileName: String, content: String): File {
    val tempDir = Files.createTempDirectory("graphql-tmp-test-dir")
    val filePath = Paths.get(tempDir.toString(), fileName)
    val f = filePath.toFile()
    val bw = BufferedWriter(FileWriter(f))
    bw.write(content)
    bw.close()
    return f
}

val serverUrl = HttpUrl.parse("http://localhost") as HttpUrl
val httpCallFactory = object : Call.Factory {
    override fun newCall(request: Request): Call {
        return object : Call {
            override fun enqueue(responseCallback: Callback) {
                TODO("not implemented")
            }

            override fun cancel() {
                TODO("not implemented")
            }

            override fun clone(): Call {
                TODO("not implemented")
            }

            override fun execute(): Response {
                TODO("not implemented")
            }

            override fun isCanceled(): Boolean {
                TODO("not implemented")
            }

            override fun isExecuted(): Boolean {
                TODO("not implemented")
            }

            override fun request(): Request {
                return request
            }
        }
    }
}
val scalarTypeAdapters = ScalarTypeAdapters(HashMap())
val logger = ApolloLogger(Optional.fromNullable(null))

val apolloServerInterceptor = ApolloServerInterceptor(serverUrl, httpCallFactory, HttpCachePolicy.NETWORK_ONLY, false,
        scalarTypeAdapters, logger, false)

fun readRequestBody(body: RequestBody): String {
    val limited: BufferedSink = Buffer()
    val source = Okio.buffer(limited)

    body.writeTo(source)
    source.flush()
    return limited.buffer().readUtf8()
}


internal var file1 = createFile("test1.txt", "content_testOne")
internal var file2 = createFile("test2.jpg", "content_testTwo")
internal var file3 = createFile("test3.pdf", "content_testThree")

class ApolloFileUploadInterceptorTest {

    @Test
    fun noUpload() {
        val operation = object : Mutation<Operation.Data, Void, Operation.Variables> {

            override fun variables(): Operation.Variables {
                return object : Operation.Variables() {
                    override fun valueMap(): Map<String, Any> {
                        return object : HashMap<String, Any>() {
                            init {
                                put("k1", "v1")
                                put("k2", "v2")
                            }
                        }
                    }
                }
            }

            override fun queryDocument(): String? {
                return "dummy request body"
            }

            override fun responseFieldMapper(): ResponseFieldMapper<Operation.Data>? {
                return null
            }

            override fun wrapData(data: Operation.Data): Void? {
                return null
            }

            override fun name(): OperationName {
                return OperationName { "" }
            }

            override fun operationId(): String {
                return ""
            }
        }


        val httpRequest = apolloServerInterceptor.httpCall(operation, CacheHeaders.NONE).request()

        assertThat(httpRequest.headers("Content-Type")[0]).isEqualTo("application/json")

        val body = httpRequest.body() as RequestBody
        val bodyString = readRequestBody(body)
        assertThat(bodyString.trimIndent()).isEqualTo("""
{"query":"dummy request body","operationName":"","variables":{}}
        """.trimIndent())

    }

    @Test
    fun singleSimpleUpload() {
        val operation = object : Mutation<Operation.Data, Void, Operation.Variables> {

            override fun variables(): Operation.Variables {
                return object : Operation.Variables() {
                    override fun valueMap(): Map<String, Any> {
                        return object : HashMap<String, Any>() {
                            init {
                                put("k1", "v1")
                                put("k2", file1)
                            }
                        }
                    }
                }
            }

            override fun queryDocument(): String? {
                return "dummy request body"
            }

            override fun responseFieldMapper(): ResponseFieldMapper<Operation.Data>? {
                return null
            }

            override fun wrapData(data: Operation.Data): Void? {
                return null
            }

            override fun name(): OperationName {
                return OperationName { "" }
            }

            override fun operationId(): String {
                return ""
            }
        }


        val httpRequest = apolloServerInterceptor.httpCall(operation, CacheHeaders.NONE).request()

        assertThat(httpRequest.headers("Content-Type")[0]).isEqualTo("multipart/form-data; boundary=--graphql-multipart-upload-boundary-85763456--")

        val body = httpRequest.body() as RequestBody
        val bodyString = readRequestBody(body)
        assertThat(bodyString.trimIndent()).isEqualTo("""
----graphql-multipart-upload-boundary-85763456--
Content-Disposition: form-data; name="operations"
Content-Type: application/json; charset=utf-8
Content-Length: 64

{"query":"dummy request body","operationName":"","variables":{}}
----graphql-multipart-upload-boundary-85763456--
Content-Disposition: form-data; name="map"
Content-Type: application/json; charset=utf-8
Content-Length: 22

{"0":["variables.k2"]}
----graphql-multipart-upload-boundary-85763456--
Content-Disposition: form-data; name="0"; filename="test1.txt"
Content-Type: text/plain
Content-Length: 15

content_testOne
----graphql-multipart-upload-boundary-85763456----
        """.trimIndent())

    }

    @Test
    fun singleNestedMapUpload() {
        val nestedObject = object : InputType {
            override fun marshaller(): InputFieldMarshaller {
                TODO("not implemented")
            }

            private val k1Lev2 = "v1"
            private val k2Lev2 = object : InputType {
                override fun marshaller(): InputFieldMarshaller {
                    TODO("not implemented")
                }

                private val k1Lev3 = "v1"
                private val k2Lev3 = file1
                private val k3Lev3 = "v3"
            }
            private val k3Lev2 = "v3"
        }
        val operation = object : Mutation<Operation.Data, Void, Operation.Variables> {
            override fun variables(): Operation.Variables {
                return object : Operation.Variables() {
                    override fun valueMap(): Map<String, Any> {
                        return object : HashMap<String, Any>() {
                            init {
                                put("k1Lev1", "v1")
                                put("k2Lev1", nestedObject)
                                put("k3Lev1", "v2")
                            }
                        }
                    }
                }
            }

            override fun queryDocument(): String? {
                return "any dummy"
            }

            override fun responseFieldMapper(): ResponseFieldMapper<Operation.Data>? {
                return null
            }

            override fun wrapData(data: Operation.Data): Void? {
                return null
            }

            override fun name(): OperationName {
                return OperationName { "" }
            }

            override fun operationId(): String {
                return ""
            }
        }
        val httpRequest = apolloServerInterceptor.httpCall(operation, CacheHeaders.NONE).request()
        val body = httpRequest.body() as RequestBody

        assertThat(httpRequest.headers("Content-Type")[0]).isEqualTo("multipart/form-data; boundary=--graphql-multipart-upload-boundary-85763456--")

        val bodyString = readRequestBody(body)
        assertThat(bodyString.trimIndent()).isEqualTo("""
----graphql-multipart-upload-boundary-85763456--
Content-Disposition: form-data; name="operations"
Content-Type: application/json; charset=utf-8
Content-Length: 55

{"query":"any dummy","operationName":"","variables":{}}
----graphql-multipart-upload-boundary-85763456--
Content-Disposition: form-data; name="map"
Content-Type: application/json; charset=utf-8
Content-Length: 40

{"0":["variables.k2Lev1.k2Lev2.k2Lev3"]}
----graphql-multipart-upload-boundary-85763456--
Content-Disposition: form-data; name="0"; filename="test1.txt"
Content-Type: text/plain
Content-Length: 15

content_testOne
----graphql-multipart-upload-boundary-85763456----
        """.trimIndent())
    }

    @Test
    fun singleNestedMapListUpload() {
        val nestedObject = object : InputType {
            override fun marshaller(): InputFieldMarshaller {
                TODO("not implemented")
            }

            private val choices = arrayListOf(object : InputType {
                override fun marshaller(): InputFieldMarshaller {
                    TODO("not implemented")
                }

                private val inputFiles = arrayListOf(file1, file2)
            }, object : InputType {
                override fun marshaller(): InputFieldMarshaller {
                    TODO("not implemented")
                }

                private val inputFiles = arrayListOf(file3)
            })
        }
        val operation = object : Mutation<Operation.Data, Void, Operation.Variables> {
            override fun variables(): Operation.Variables {
                return object : Operation.Variables() {
                    override fun valueMap(): Map<String, Any> {
                        return object : HashMap<String, Any>() {
                            init {
                                put("answer", nestedObject)
                            }
                        }
                    }
                }
            }

            override fun queryDocument(): String? {
                return "singleNestedMapListUpload"
            }

            override fun responseFieldMapper(): ResponseFieldMapper<Operation.Data>? {
                return null
            }

            override fun wrapData(data: Operation.Data): Void? {
                return null
            }

            override fun name(): OperationName {
                return OperationName { "" }
            }

            override fun operationId(): String {
                return ""
            }
        }


        val httpRequest = apolloServerInterceptor.httpCall(operation, CacheHeaders.NONE).request()
        val body = httpRequest.body() as RequestBody

        assertThat(httpRequest.headers("Content-Type")[0]).isEqualTo("multipart/form-data; boundary=--graphql-multipart-upload-boundary-85763456--")

        val bodyString = readRequestBody(body)
        assertThat(bodyString.trimIndent()).isEqualTo("""
----graphql-multipart-upload-boundary-85763456--
Content-Disposition: form-data; name="operations"
Content-Type: application/json; charset=utf-8
Content-Length: 71

{"query":"singleNestedMapListUpload","operationName":"","variables":{}}
----graphql-multipart-upload-boundary-85763456--
Content-Disposition: form-data; name="map"
Content-Type: application/json; charset=utf-8
Content-Length: 145

{"0":["variables.answer.choices.0.inputFiles.0"],"1":["variables.answer.choices.0.inputFiles.1"],"2":["variables.answer.choices.1.inputFiles.0"]}
----graphql-multipart-upload-boundary-85763456--
Content-Disposition: form-data; name="0"; filename="test1.txt"
Content-Type: text/plain
Content-Length: 15

content_testOne
----graphql-multipart-upload-boundary-85763456--
Content-Disposition: form-data; name="1"; filename="test2.jpg"
Content-Type: image/jpeg
Content-Length: 15

content_testTwo
----graphql-multipart-upload-boundary-85763456--
Content-Disposition: form-data; name="2"; filename="test3.pdf"
Content-Type: application/pdf
Content-Length: 17

content_testThree
----graphql-multipart-upload-boundary-85763456----
        """.trimIndent())
    }
}
