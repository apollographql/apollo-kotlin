package com.apollographql.apollo.tooling

import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter

internal object VoidAdapter : Adapter<Unit> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Unit {
    return Unit
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Unit) {
    writer.nullValue()
  }
}
