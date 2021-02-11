package com.apollographql.apollo.testing

import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.ResponseAdapter
import com.apollographql.apollo.api.internal.ResponseReader
import com.apollographql.apollo.api.internal.ResponseWriter

class MockQuery : Query<MockQuery.Data> {

  override fun queryDocument(): String = "query MockQuery { name }"

  override fun variables(): Operation.Variables = Operation.EMPTY_VARIABLES

  override fun adapter(): ResponseAdapter<Data> {
    return object : ResponseAdapter<Data> {
      override fun fromResponse(reader: ResponseReader, __typename: String?): Data {
        while (reader.selectField(emptyArray()) != -1) {
          // consume the json stream
        }
        return Data
      }

      override fun toResponse(writer: ResponseWriter, value: Data) {
        TODO("Not yet implemented")
      }
    }
  }

  override fun name(): String = "MockQuery"

  override fun operationId(): String = "MockQuery".hashCode().toString()

  object Data : Operation.Data

  override fun responseFields(): List<ResponseField.FieldSet> {
    return emptyList()
  }

}
