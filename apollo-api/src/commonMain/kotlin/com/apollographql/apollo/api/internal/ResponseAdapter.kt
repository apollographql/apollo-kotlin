package com.apollographql.apollo.api.internal

import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.internal.json.JsonWriter

interface ResponseAdapter<T> {

  fun fromResponse(reader: ResponseReader, __typename: String? = null): T

  fun toResponse(writer: JsonWriter, value: T, customScalarAdapters: CustomScalarAdapters)
}
