package com.apollographql.apollo.testing

import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.OperationName
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.Subscription
import com.apollographql.apollo.api.internal.ResponseFieldMapper
import com.apollographql.apollo.api.internal.ResponseFieldMarshaller

class MockSubscription : Subscription<MockSubscription.Data, Operation.Variables> {

  override fun queryDocument(): String = "subscription MockSubscription { name }"

  override fun variables(): Operation.Variables = Operation.EMPTY_VARIABLES

  override fun responseFieldMapper(): ResponseFieldMapper<Data> {
    return ResponseFieldMapper { reader ->
      Data(
          name = reader.readString(
              ResponseField.forString(
                  responseName = "name",
                  fieldName = "name",
                  arguments = null,
                  optional = false,
                  conditions = null
              )
          )!!
      )
    }
  }

  override fun name(): OperationName = object : OperationName {
    override fun name(): String = "MockSubscription"
  }

  override fun operationId(): String = "MockSubscription".hashCode().toString()

  data class Data(val name: String) : Operation.Data {
    override fun marshaller(): ResponseFieldMarshaller {
      throw UnsupportedOperationException("Unsupported")
    }
  }
}
