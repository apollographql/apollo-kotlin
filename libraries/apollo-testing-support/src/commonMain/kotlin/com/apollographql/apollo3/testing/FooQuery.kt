package com.apollographql.apollo3.testing

import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.CompiledField
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.checkFieldNotMissing
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.api.missingField

class FooQuery: Query<FooQuery.Data> {
  class Data(val foo: Int): Query.Data

  override fun document(): String {
    return "query GetFoo { foo }"
  }

  override fun name(): String {
    return "FooQuery"
  }

  override fun id(): String {
    return "0"
  }

  override fun adapter(): Adapter<Data> {
    return object :Adapter<Data> {
      override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Data {
        var foo: Int? = null
        reader.beginObject()
        while (reader.hasNext()) {
          when (reader.nextName()) {
            "foo" -> {
              foo = reader.nextInt()
            }
            else -> reader.skipValue()
          }
        }
        reader.endObject()
        checkFieldNotMissing(foo, "foo")
        return Data(foo ?: missingField(reader, "foo"))
      }

      override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Data) {
        writer.name("foo")
        writer.value(value.foo)
      }
    }
  }

  override fun serializeVariables(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, withDefaultValues: Boolean) {
  }

  override fun rootField(): CompiledField {
    TODO("Not yet implemented")
  }

  override val ignoreErrors: Boolean
    get() = false

  companion object {
    val successResponse = "{\"data\": {\"foo\": 42}}"
  }
}

