package com.apollographql.apollo.api.internal

import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.internal.json.JsonReader
import com.apollographql.apollo.api.internal.json.JsonWriter

interface ResponseAdapter<T> {

  fun fromResponse(reader: JsonReader): T

  fun toResponse(writer: JsonWriter, value: T)
}
