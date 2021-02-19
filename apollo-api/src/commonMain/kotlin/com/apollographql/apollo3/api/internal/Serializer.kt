package com.apollographql.apollo3.api.internal

import com.apollographql.apollo3.api.internal.json.JsonWriter

interface Serializer<T> {
  fun toJson(writer: JsonWriter, value: T)
}