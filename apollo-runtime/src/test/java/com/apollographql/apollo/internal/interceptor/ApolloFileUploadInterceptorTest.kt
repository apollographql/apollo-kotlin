package com.apollographql.apollo.internal.interceptor

import com.apollographql.apollo.api.*
import com.apollographql.apollo.internal.interceptor.ApolloFileUploadInterceptor.httpMultipartRequestBody
import com.google.common.truth.Truth.assertThat
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import okio.Okio
import org.junit.Test
import java.io.BufferedWriter
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

fun createGraphqlUpload(fileName: String, content: String, mimeType: String): GraphqlUpload {
    val tempDir = Files.createTempDirectory("graphql-tmp-test-dir")
    val filePath = Paths.get(tempDir.toString(), fileName)
    val f = filePath.toFile()
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

fun readRequestBody(body: RequestBody): String {
    val limited: BufferedSink = Buffer()
    val source = Okio.buffer(limited)

    body.writeTo(source)
    source.flush()
    return limited.buffer().readUtf8()
}


internal var file1 = createGraphqlUpload("test1.txt", "content_testOne", "text/plain")
internal var file2 = createGraphqlUpload("test2.jpg", "content_testTwo", "image/jpeg")
internal var file3 = createGraphqlUpload("test3.pdf", "content_testThree", "text/plain")
internal var boundary = "----graphql-multipart-upload-boundary-85763456--"

class ApolloFileUploadInterceptorTest {

    @Test
    fun singleSimpleUpload() {
        val mainBody = RequestBody.create(null, "dummy request body")
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
                return null
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


        val body = httpMultipartRequestBody(mainBody, operation)
        val bodyString = readRequestBody(body)
        assertThat(bodyString.trimIndent()).isEqualTo("""
----graphql-multipart-upload-boundary-85763456--
Content-Disposition: form-data; name="operations"
Content-Length: 18

dummy request body
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
        val mainBody = RequestBody.create(null, "dummy request body")
        val operation = object : Mutation<Operation.Data, Void, Operation.Variables> {
            override fun variables(): Operation.Variables {
                return object : Operation.Variables() {
                    override fun valueMap(): Map<String, Any> {
                        return object : HashMap<String, Any>() {
                            init {
                                put("k1Lev1", "v1")
                                put("k2Lev1", object : HashMap<String, Any>() {
                                    init {
                                        put("k1Lev2", "v1")
                                        put("k2Lev2", object : HashMap<String, Any>() {
                                            init {
                                                put("k1Lev3", "v1")
                                                put("k2Lev3", file1)
                                                put("k3Lev3", "v3")
                                            }
                                        })
                                        put("k3Lev2", "v3")
                                    }
                                })
                                put("k3Lev1", "v2")
                            }
                        }
                    }
                }
            }

            override fun queryDocument(): String? {
                return null
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


        val body = httpMultipartRequestBody(mainBody, operation)
        val bodyString = readRequestBody(body)
        assertThat(bodyString.trimIndent()).isEqualTo("""
----graphql-multipart-upload-boundary-85763456--
Content-Disposition: form-data; name="operations"
Content-Length: 18

dummy request body
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
    // “variables.answer.inputFiles.0”


    @Test
    fun singleNestedMapListUpload() {
        val mainBody = RequestBody.create(null, "dummy request body")
        val operation = object : Mutation<Operation.Data, Void, Operation.Variables> {
            override fun variables(): Operation.Variables {
                return object : Operation.Variables() {
                    override fun valueMap(): Map<String, Any> {
                        return object : HashMap<String, Any>() {
                            //“variables.answer.choices.0.inputFiles.0”
                            init {
                                put("answer", object : HashMap<String, Any>() {
                                    init {
                                        put("choices", arrayListOf(object : HashMap<String, Any>() {
                                            init {
                                                put("inputFiles", arrayListOf(file1, file2))
                                            }
                                        }, object : HashMap<String, Any>() {
                                            init {
                                                put("inputFiles", arrayListOf(file3))
                                            }
                                        }))
                                    }
                                })
                            }
                        }
                    }
                }
            }

            override fun queryDocument(): String? {
                return null
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


        val body = httpMultipartRequestBody(mainBody, operation)
        val bodyString = readRequestBody(body)
        assertThat(bodyString.trimIndent()).isEqualTo("""
----graphql-multipart-upload-boundary-85763456--
Content-Disposition: form-data; name="operations"
Content-Length: 18

dummy request body
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
Content-Type: text/plain
Content-Length: 17

content_testThree
----graphql-multipart-upload-boundary-85763456----
        """.trimIndent())
    }
}
