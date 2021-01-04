package com.apollographql.apollo.testing

import com.apollographql.apollo.ApolloParseException
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.OperationName
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.internal.ResponseFieldMapper
import com.apollographql.apollo.api.internal.ResponseFieldMarshaller
import okio.BufferedSource
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8

class MockQuery : Query<MockQuery.Data, Operation.Variables> {

  override fun composeRequestBody(
      autoPersistQueries: Boolean,
      withQueryDocument: Boolean,
      customScalarAdapters: CustomScalarAdapters
  ): ByteString {
    return composeRequestBody()
  }

  override fun composeRequestBody(customScalarAdapters: CustomScalarAdapters): ByteString {
    return composeRequestBody()
  }

  override fun composeRequestBody(): ByteString {
    return """
    { 
      "operationName": "MockQuery",
      "query": "query MockQuery { name }",
      "variables": "{"key": "value"}"
    }
    """.trimIndent().encodeUtf8()
  }

  override fun queryDocument(): String = "query MockQuery { name }"

  override fun variables(): Operation.Variables = Operation.EMPTY_VARIABLES

  override fun responseFieldMapper(): ResponseFieldMapper<Data> {
    throw UnsupportedOperationException("Unsupported")
  }

  override fun name(): OperationName = object : OperationName {
    override fun name(): String = "MockQuery"
  }

  override fun operationId(): String = "MockQuery".hashCode().toString()

  override fun parse(source: BufferedSource, customScalarAdapters: CustomScalarAdapters): Response<Data> {
    val data = source.readUtf8()
    if (data.isEmpty()) throw ApolloParseException(
        message = "Failed to parse mocked response"
    )
    return Response(
        operation = this,
        data = Data(data)
    )
  }

  override fun parse(byteString: ByteString, customScalarAdapters: CustomScalarAdapters): Response<Data> {
    if (byteString.size == 0) throw ApolloParseException(
        message = "Failed to parse mocked response"
    )
    return Response(
        operation = this,
        data = Data(byteString.utf8())
    )
  }

  override fun parse(source: BufferedSource): Response<Data> {
    return parse(source, CustomScalarAdapters.DEFAULT)
  }

  override fun parse(byteString: ByteString): Response<Data> {
    return parse(byteString, CustomScalarAdapters.DEFAULT)
  }

  data class Data(val rawResponse: String) : Operation.Data {
    override fun marshaller(): ResponseFieldMarshaller {
      throw UnsupportedOperationException("Unsupported")
    }
  }
}
