package com.apollographql.apollo.tooling

import com.apollographql.apollo.api.Adapter
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.json.JsonReader
import com.apollographql.apollo.api.json.JsonWriter

internal object VoidAdapter : Adapter<Unit> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Unit {
    return Unit
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Unit) {
    writer.nullValue()
  }
}
