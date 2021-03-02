package com.apollographql.apollo3.testing

import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.ResponseField
import com.apollographql.apollo3.api.Subscription
import com.apollographql.apollo3.api.AnyResponseAdapter
import com.apollographql.apollo3.api.ResponseAdapter
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter

@Deprecated("Tests using MockQuery are very fragile to codegen changes, use integration tests instead")
class MockSubscription(
    private val queryDocument: String = "subscription MockSubscription { name }",
    private val variables: Map<String, Any?> = emptyMap(),
    private val name: String = "MockSubscription",
) : Subscription<MockSubscription.Data> {

  override fun queryDocument(): String = queryDocument

  override fun serializeVariables(writer: JsonWriter, responseAdapterCache: ResponseAdapterCache) {
    AnyResponseAdapter.toResponse(writer, variables)
  }

  override fun adapter(responseAdapterCache: ResponseAdapterCache): ResponseAdapter<Data> {
    return object : ResponseAdapter<Data> {
      override fun fromResponse(reader: JsonReader): Data {
        reader.beginObject()
        reader.nextName()
        return Data(
            name = reader.nextString()!!
        ).also {
          reader.endObject()
        }
      }

      override fun toResponse(writer: JsonWriter, value: Data) {
        TODO("Not yet implemented")
      }
    }
  }

  override fun name(): String = name

  override fun operationId(): String = name.hashCode().toString()

  data class Data(val name: String) : Subscription.Data

  override fun responseFields(): List<ResponseField.FieldSet> {
    return emptyList()
  }
}
