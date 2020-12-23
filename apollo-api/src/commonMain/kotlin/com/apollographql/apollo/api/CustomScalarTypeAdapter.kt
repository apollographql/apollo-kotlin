package com.apollographql.apollo.api

/**
 * Class represents the adapter for mapping GraphQL custom scalar types to Java objects.
 *
 * The GraphQL specification includes the following default scalar types: **String, Int, Float and Boolean**. But
 * often use cases arise when support for custom scalar types is required and for those cases this class should be used
 * to customize conversion. Here's an example of a type adapter for scalar type `java.util.Date`:
 *
 * ```
 *    val adapter = object : CustomTypeAdapter<Date> {
 *
 *      override fun(value: CustomTypeValue<*>): Date {
 *        try {
 *          return ISO8601_DATE_FORMAT.parse(value);
 *        } catch (ParseException e) {
 *          throw new RuntimeException(e);
 *        }
 *      }
 *
 *      override fun encode(value: Date): CustomTypeValue {
 *        return ISO8601_DATE_FORMAT.format(value);
 *      }
 *   }
 * ```
 */
interface CustomScalarTypeAdapter<T> {

  /**
   * De-serializes the [value] to the custom scalar type [T]. Usually used in parsing the GraphQL response.
   */
  fun decode(value: CustomTypeValue<*>): T

  /**
   * Serializes the custom scalar type [value] to the corresponding [CustomTypeValue]. Usually used in serializing variables or input
   * values.
   */
  fun encode(value: T): CustomTypeValue<*>
}
