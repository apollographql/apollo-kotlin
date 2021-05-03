package com.apollographql.apollo3.testing

import com.apollographql.apollo3.api.CustomScalarAdpaters
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.MergedField
import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter

@Deprecated("Tests using MockQuery are very fragile to codegen changes, use integration tests instead")
class MockQuery : Query<MockQuery.Data> {

  override fun document(): String = "query MockQuery { name }"

  override fun serializeVariables(writer: JsonWriter, responseAdapterCache: CustomScalarAdpaters) {
  }

  override fun adapter(): Adapter<Data> {
    return object : Adapter<Data> {
      override fun fromJson(reader: JsonReader, responseAdapterCache: CustomScalarAdpaters): Data {
        reader.beginObject()
        // consume the json stream
        while (reader.hasNext()) {
          reader.nextName()
          reader.skipValue()
        }
        reader.endObject()
        return Data
      }

      override fun toJson(writer: JsonWriter, responseAdapterCache: CustomScalarAdpaters, value: Data) {
        TODO("Not yet implemented")
      }
    }
  }

  override fun name(): String = "MockQuery"

  override fun id(): String = "MockQuery".hashCode().toString()

  object Data : Query.Data

  override fun responseFields(): List<MergedField.FieldSet> {
    return emptyList()
  }
}
