package com.apollographql.apollo3.adapter

import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter

/**
 * An [Adapter] that converts a Json number to/from a Java [Long]
 *
 * If the Json number does not fit in a [Long], an exception will be thrown
 *
 */
object LongAdapter: Adapter<Long>  {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Long {
    return reader.nextString()!!.toLong()
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Long) {
    writer.value(value.toString())
  }
}