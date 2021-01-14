package com.apollographql.apollo.api

val EMPTY_OPERATION: Operation<*> = object : Operation<Operation.Data> {
  override fun variables(): Operation.Variables {
    return Operation.EMPTY_VARIABLES
  }

  override fun name(): String = "test"

  override fun operationId() = ""

  override fun queryDocument() = throw UnsupportedOperationException()
  override fun adapter() = throw UnsupportedOperationException()
}
