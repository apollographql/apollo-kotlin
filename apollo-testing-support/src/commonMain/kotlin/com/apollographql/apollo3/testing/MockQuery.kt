package com.apollographql.apollo3.testing

import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.ResponseField
import com.apollographql.apollo3.api.internal.ResponseAdapter
import com.apollographql.apollo3.api.internal.json.JsonReader
import com.apollographql.apollo3.api.internal.json.JsonWriter

class MockQuery : Query<MockQuery.Data> {

  override fun queryDocument(): String = "query MockQuery { name }"

  override fun serializeVariables(writer: JsonWriter, responseAdapterCache: ResponseAdapterCache) {
  }

  override fun adapter(responseAdapterCache: ResponseAdapterCache): ResponseAdapter<Data> {
    return object : ResponseAdapter<Data> {
      override fun fromResponse(reader: JsonReader): Data {
        reader.beginObject()
        // consume the json stream
        while (reader.hasNext()) {
          reader.nextName()
          reader.skipValue()
        }
        reader.endObject()
        return Data
      }

      override fun toResponse(writer: JsonWriter, value: Data) {
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
