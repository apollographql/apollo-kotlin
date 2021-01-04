package com.apollographql.apollo.testing

import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.OperationName
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.internal.ResponseFieldMapper
import com.apollographql.apollo.api.internal.ResponseFieldMarshaller

class MockQuery : Query<MockQuery.Data, Operation.Variables> {

  override fun queryDocument(): String = "query MockQuery { name }"

  override fun variables(): Operation.Variables = Operation.EMPTY_VARIABLES

  override fun responseFieldMapper(): ResponseFieldMapper<Data> {
    return ResponseFieldMapper {
      while (it.selectField(emptyArray()) != -1) {
        // consume the json stream
      }
      Data
    }
  }

  override fun name(): OperationName = object : OperationName {
    override fun name(): String = "MockQuery"
  }

  override fun operationId(): String = "MockQuery".hashCode().toString()

  object Data : Operation.Data {

    override fun marshaller(): ResponseFieldMarshaller {
      throw UnsupportedOperationException("Unsupported")
    }
  }
}
