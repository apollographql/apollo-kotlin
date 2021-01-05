package com.apollographql.apollo.testing

import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.OperationName
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.Subscription
import com.apollographql.apollo.api.internal.ResponseAdapter
import com.apollographql.apollo.api.internal.ResponseReader
import com.apollographql.apollo.api.internal.ResponseWriter

class MockSubscription : Subscription<MockSubscription.Data> {

  override fun queryDocument(): String = "subscription MockSubscription { name }"

  override fun variables(): Operation.Variables = Operation.EMPTY_VARIABLES

  override fun adapter(): ResponseAdapter<Data> {
    return object: ResponseAdapter<Data> {
      override fun fromResponse(reader: ResponseReader, __typename: String?): Data {
        return Data(
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

      override fun toResponse(writer: ResponseWriter, value: Data) {
        TODO("Not yet implemented")
      }
    }
  }

  override fun name(): OperationName = object : OperationName {
    override fun name(): String = "MockSubscription"
  }

  override fun operationId(): String = "MockSubscription".hashCode().toString()

  data class Data(val name: String) : Operation.Data
}
