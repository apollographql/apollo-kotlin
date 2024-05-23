package com.apollographql.apollo3.network.adapter

import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import io.ktor.http.Url

/**
 * An [Adapter] that converts to/from [io.ktor.http.Url]
 */
object KtorHttpUrlAdapter: Adapter<Url> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Url {
    return Url(reader.nextString()!!)
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Url) {
    writer.value(value.toString())
  }
}

