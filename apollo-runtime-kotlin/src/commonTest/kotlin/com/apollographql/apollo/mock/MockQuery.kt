package com.apollographql.apollo.mock

import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.OperationName
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.api.ScalarTypeAdapters
import com.apollographql.apollo.api.internal.ResponseFieldMapper
import com.apollographql.apollo.api.internal.ResponseFieldMarshaller
import okio.BufferedSource
import okio.ByteString

internal class MockQuery : Query<MockQuery.Data, MockQuery.Data, Operation.Variables> {

  override fun composeRequestBody(
      autoPersistQueries: Boolean,
      withQueryDocument: Boolean,
      scalarTypeAdapters: ScalarTypeAdapters
  ): ByteString {
    return ByteString.of()
  }

  override fun queryDocument(): String = "query MockQuery { name }"

  override fun variables(): Operation.Variables = Operation.EMPTY_VARIABLES

  override fun responseFieldMapper(): ResponseFieldMapper<Data> = ResponseFieldMapper.invoke {
    Data
  }

  override fun wrapData(data: Data?): Data? = data

  override fun name(): OperationName = object : OperationName {
    override fun name(): String = "MockQuery"
  }

  override fun operationId(): String = "operationId"

  override fun parse(source: BufferedSource, scalarTypeAdapters: ScalarTypeAdapters): Response<Data> {
    require(source.readUtf8() == "{\"data\":{\"name\":\"MockQuery\"}}")
    return Response(
        operation = this,
        data = Data
    )
  }

  override fun parse(byteString: ByteString, scalarTypeAdapters: ScalarTypeAdapters): Response<Data> {
    require(byteString.toString() == "{\"data\":{\"name\":\"MockQuery\"}}")
    return Response(
        operation = this,
        data = Data
    )
  }

  override fun parse(source: BufferedSource): Response<Data> {
    return parse(source, ScalarTypeAdapters.DEFAULT)
  }

  override fun parse(byteString: ByteString): Response<Data> {
    return parse(byteString, ScalarTypeAdapters.DEFAULT)
  }

  override fun composeRequestBody(scalarTypeAdapters: ScalarTypeAdapters): ByteString {
    throw UnsupportedOperationException("Unsupported")
  }

  override fun composeRequestBody(): ByteString {
    throw UnsupportedOperationException("Unsupported")
  }

  object Data : Operation.Data {
    override fun marshaller(): ResponseFieldMarshaller {
      throw UnsupportedOperationException("Unsupported")
    }
  }
}
