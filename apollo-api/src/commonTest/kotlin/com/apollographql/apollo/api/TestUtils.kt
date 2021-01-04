package com.apollographql.apollo.api

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
}
