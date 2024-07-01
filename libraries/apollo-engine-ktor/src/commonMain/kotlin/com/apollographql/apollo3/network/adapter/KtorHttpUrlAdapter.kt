package com.apollographql.apollo.network.adapter

import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.api.Adapter
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.json.JsonReader
import com.apollographql.apollo.api.json.JsonWriter
import io.ktor.http.Url

/**
 * An [Adapter] that converts to/from [io.ktor.http.Url]
 */
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
@Deprecated("apollo-engine-ktor has moved to 'com.apollographql.ktor:apollo-engine-ktor'")
object KtorHttpUrlAdapter: Adapter<Url> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Url {
    return Url(reader.nextString()!!)
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Url) {
    writer.value(value.toString())
  }
}

