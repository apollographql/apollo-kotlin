package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.api.json.buildJsonString

/**
 *
 */
expect fun Operation.Data.toJson(jsonWriter: JsonWriter, scalarAdapters: ScalarAdapters = ScalarAdapters.Empty)

fun Operation.Data.toJson(scalarAdapters: ScalarAdapters = ScalarAdapters.Empty): String = buildJsonString {
  toJson(this, scalarAdapters)
}

fun Operation.Data.toResponseJson(scalarAdapters: ScalarAdapters = ScalarAdapters.Empty): String = buildJsonString {
  beginObject()
  name("data")
  toJson(this, scalarAdapters)
  endObject()
}
