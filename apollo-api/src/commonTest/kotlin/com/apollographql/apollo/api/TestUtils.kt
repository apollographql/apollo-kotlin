package com.apollographql.apollo.api

import com.apollographql.apollo.api.internal.ResponseAdapter

val EMPTY_OPERATION: Operation<*, *> = object : Operation<Operation.Data, Operation.Variables> {
  override fun variables(): Operation.Variables {
    return Operation.EMPTY_VARIABLES
  }

  override fun name(): OperationName = object : OperationName {
    override fun name() = "test"
  }

  override fun operationId() = ""

  override fun queryDocument() = throw UnsupportedOperationException()
  override fun adapter() = throw UnsupportedOperationException()
}
