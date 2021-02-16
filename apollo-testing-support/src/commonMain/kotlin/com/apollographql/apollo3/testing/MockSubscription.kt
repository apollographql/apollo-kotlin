package com.apollographql.apollo.testing

import com.apollographql.apollo.api.ResponseAdapterCache
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.Subscription
import com.apollographql.apollo.api.internal.InputFieldMarshaller
import com.apollographql.apollo.api.internal.ResponseAdapter
import com.apollographql.apollo.api.internal.json.JsonReader
import com.apollographql.apollo.api.internal.json.JsonWriter

class MockSubscription(
    private val queryDocument: String = "subscription MockSubscription { name }",
    private val variables: Map<String, Any?> = emptyMap(),
    private val name: String = "MockSubscription",
) : Subscription<MockSubscription.Data> {

  override fun queryDocument(): String = queryDocument

  override fun variables(): Operation.Variables = object: Operation.Variables() {
    override fun valueMap() = variables

    override fun marshaller(): InputFieldMarshaller =
        InputFieldMarshaller { writer ->
          for ((name, value) in variables.entries) {
            when (value) {
              is Number -> writer.writeNumber(name, value)
              is Boolean -> writer.writeBoolean(name, value)
              else -> writer.writeString(name, value.toString())
            }
          }
        }
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

  data class Data(val name: String) : Operation.Data

  override fun responseFields(): List<ResponseField.FieldSet> {
    return emptyList()
  }
}
