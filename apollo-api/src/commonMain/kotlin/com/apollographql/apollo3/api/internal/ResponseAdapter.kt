package com.apollographql.apollo3.api.internal

import com.apollographql.apollo3.api.internal.json.JsonReader
import com.apollographql.apollo3.api.internal.json.JsonWriter

interface ResponseAdapter<T>: ResponseSerializer<T> {
  fun fromResponse(reader: JsonReader): T
}
