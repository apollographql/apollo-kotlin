package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.api.json.buildJsonString

/**
 *
 */
expect fun Operation.Data.toJson(jsonWriter: JsonWriter, customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty)

fun Operation.Data.toJson(customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty): String = buildJsonString {
  toJson(this, customScalarAdapters)
}