package com.apollographql.apollo3.api

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import com.apollographql.apollo3.annotations.ApolloDeprecatedSince.Version.v4_0_0
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import okio.IOException

@Deprecated("Use ScalarAdapter instead")
@ApolloDeprecatedSince(v4_0_0)
@Suppress("DEPRECATION")
interface Adapter<T> {
  @Throws(IOException::class)
  fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): T

  @Throws(IOException::class)
  fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: T)
}
