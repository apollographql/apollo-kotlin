package com.apollographql.apollo.api

/**
 * Class represents the adapter for mapping GraphQL custom scalar types to Java objects.
 *
 * The GraphQL specification includes the following default scalar types: **String, Int, Float and Boolean**. But
 * often use cases arise when support for custom scalar types is required and for those cases this class should be used
 * to customize conversion. Here's an example of a type adapter for scalar type `java.util.Date`:
 *
 * ```
 *    val adapter = object : CustomScalarTypeAdapter<Date> {
 *
 *      override fun(jsonElement: JsonElement): Date {
 *        try {
 *          return ISO8601_DATE_FORMAT.parse(value);
 *        } catch (ParseException e) {
 *          throw new RuntimeException(e);
 *        }
 *      }
 *
 *      override fun encode(value: Date): JsonElement {
 *        return ISO8601_DATE_FORMAT.format(value);
 *      }
 *   }
 * ```
 */
interface CustomScalarTypeAdapter<T> {

  /**
   * De-serializes the [value] to the custom scalar type [T]. Usually used in parsing the GraphQL response.
   *
   * null are handled by the caller so jsonElement will never be [JsonElement.JsonNull] and this method must always return a non-null value.
   * If jsonElement is an instance of [JsonElement.JsonObject] or [JsonElement.JsonList], it can contain [JsonElement.JsonNull]
   *
   * Note: we do not support mapping null json values to non-null kotlin values or the other way around
   */
  fun decode(jsonElement: JsonElement): T

  /**
   * Serializes the custom scalar type [value] to the corresponding [JsonElement]. Used when serializing variables, input
   * values or for storing in the cache.
   */
  fun encode(value: T): JsonElement
}
