package com.apollographql.apollo.api

import com.apollographql.apollo.api.json.JsonWriter
import com.apollographql.apollo.api.json.buildJsonString

/**
 * Serializes the given `Data` to the given [JsonWriter].
 *
 * Note: this method uses reflection to lookup the adapter. If you are using R8, add the following rules:
 *
 * ```
 * -keep class ** implements com.apollographql.apollo.api.Operation$Data
 * -keep class **.*_ResponseAdapter$Data {
 *     public static ** INSTANCE;
 * }
 * ```
 *
 * @param customScalarAdapters the adapters to use for custom scalars
 */
expect fun Operation.Data.toJson(jsonWriter: JsonWriter, customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty)

/**
 * Serializes the given `Data` to a String.
 *
 * Note: this method uses reflection to lookup the adapter. If you are using R8, add the following rules:
 *
 * ```
 * -keep class ** implements com.apollographql.apollo.api.Operation$Data
 * -keep class **.*_ResponseAdapter$Data {
 *     public static ** INSTANCE;
 * }
 * ```
 *
 * @param customScalarAdapters the adapters to use for custom scalars
 */
fun Operation.Data.toJson(customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty): String = buildJsonString {
  toJson(this, customScalarAdapters)
}

/**
 * Serializes the given `Data` to a successful GraphQL response:
 *
 * ```json
 * {
 *   "data": ...
 * }
 * ```
 *
 * Note: this method uses reflection to lookup the adapter. If you are using R8, add the following rules:
 *
 * ```
 * -keep class ** implements com.apollographql.apollo.api.Operation$Data
 * -keep class **.*_ResponseAdapter$Data {
 *     public static ** INSTANCE;
 * }
 * ```
 *
 * @param customScalarAdapters the adapters to use for custom scalars
 */
fun Operation.Data.toResponseJson(customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty): String = buildJsonString {
  beginObject()
  name("data")
  toJson(this, customScalarAdapters)
  endObject()
}
