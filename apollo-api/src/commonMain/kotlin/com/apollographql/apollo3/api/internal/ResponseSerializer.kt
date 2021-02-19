package com.apollographql.apollo3.api.internal

import com.apollographql.apollo3.api.internal.json.JsonWriter

interface ResponseSerializer<T> {
  fun toResponse(writer: JsonWriter, value: T)
}