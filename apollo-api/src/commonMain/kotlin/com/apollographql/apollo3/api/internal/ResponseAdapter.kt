package com.apollographql.apollo3.api.internal

import com.apollographql.apollo3.api.internal.json.JsonReader
import com.apollographql.apollo3.api.internal.json.JsonWriter

interface ResponseAdapter<T> {
  fun fromResponse(reader: JsonReader): T
  fun toResponse(writer: JsonWriter, value: T)
}
