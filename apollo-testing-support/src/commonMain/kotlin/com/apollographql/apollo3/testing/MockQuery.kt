package com.apollographql.apollo3.testing

import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.CompiledSelection
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter

@Deprecated("Tests using MockQuery are very fragile to codegen changes, use integration tests instead")
class MockQuery : Query<MockQuery.Data> {

  override fun document(): String = "query MockQuery { name }"

  override fun serializeVariables(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters) {
  }

  override fun adapter(): Adapter<Data> {
    return object : Adapter<Data> {
      override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Data {
        reader.beginObject()
        // consume the json stream
        while (reader.hasNext()) {
          reader.nextName()
          reader.skipValue()
        }
        reader.endObject()
        return Data
      }

      override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Data) {
        TODO("Not yet implemented")
      }
    }
  }

  override fun name(): String = "MockQuery"

  override fun id(): String = "MockQuery".hashCode().toString()

  object Data : Query.Data

  override fun selections(): List<CompiledSelection> {
    return emptyList()
  }
}
