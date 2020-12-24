package com.apollographql.apollo.api

import okio.BufferedSource
import okio.ByteString

val EMPTY_OPERATION: Operation<*, *> = object : Operation<Operation.Data, Operation.Variables> {
  override fun variables(): Operation.Variables {
    return Operation.EMPTY_VARIABLES
  }

  override fun name(): OperationName = object : OperationName {
    override fun name() = "test"
  }

  override fun operationId() = ""

  override fun queryDocument() = throw UnsupportedOperationException()
  override fun responseFieldMapper() = throw UnsupportedOperationException()
  override fun parse(source: BufferedSource) = throw UnsupportedOperationException()
  override fun parse(source: BufferedSource, customScalarAdapters: CustomScalarAdapters) = throw UnsupportedOperationException()
  override fun parse(byteString: ByteString) = throw UnsupportedOperationException()
  override fun parse(byteString: ByteString, customScalarAdapters: CustomScalarAdapters) = throw UnsupportedOperationException()
  override fun composeRequestBody() = throw UnsupportedOperationException()
  override fun composeRequestBody(customScalarAdapters: CustomScalarAdapters) = throw UnsupportedOperationException()
  override fun composeRequestBody(autoPersistQueries: Boolean, withQueryDocument: Boolean, customScalarAdapters: CustomScalarAdapters) = throw UnsupportedOperationException()
}
