package com.apollographql.apollo3.testing

import com.apollographql.apollo3.api.CustomScalarAdpaters
import com.apollographql.apollo3.api.MergedField
import com.apollographql.apollo3.api.Subscription
import com.apollographql.apollo3.api.AnyAdapter
import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.api.nullable

@Deprecated("Tests using MockQuery are very fragile to codegen changes, use integration tests instead")
class MockSubscription(
    private val queryDocument: String = "subscription MockSubscription { name }",
    private val variables: Map<String, Any?> = emptyMap(),
    private val name: String = "MockSubscription",
) : Subscription<MockSubscription.Data> {

  override fun document(): String = queryDocument

  override fun serializeVariables(writer: JsonWriter, responseAdapterCache: CustomScalarAdpaters) {
    variables.forEach {
      writer.name(it.key)
      AnyAdapter.nullable().toJson(writer, responseAdapterCache, it.value)
    }
  }

  override fun adapter(): Adapter<Data> {
    return object : Adapter<Data> {
      override fun fromJson(reader: JsonReader, responseAdapterCache: CustomScalarAdpaters): Data {
        reader.beginObject()
        reader.nextName()
        return Data(
            name = reader.nextString()!!
        ).also {
          reader.endObject()
        }
      }

      override fun toJson(writer: JsonWriter, responseAdapterCache: CustomScalarAdpaters, value: Data) {
        TODO("Not yet implemented")
      }
    }
  }

  override fun name(): String = name

  override fun id(): String = name.hashCode().toString()

  data class Data(val name: String) : Subscription.Data

  override fun responseFields(): List<MergedField.FieldSet> {
    return emptyList()
  }
}
