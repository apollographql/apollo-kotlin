package com.apollographql.apollo.api

/**
 * Represents an error response returned from the GraphQL server
 */
class Error(
    /**
     * Server error message
     */
    val message: String,

    /**
     * Locations of the errors in the GraphQL operation
     */
    val locations: List<Location> = emptyList(),

    /**
     * Custom attributes associated with this error
     */
    val customAttributes: Map<String, Any?> = emptyMap()
) {

  /**
   * Returns server error message.
   */
  @Deprecated(message = "Use property instead", replaceWith = ReplaceWith(expression = "message"))
  fun message(): String? = message

  /**
   * Returns the location of the error in the GraphQL operation.
   */
  @Deprecated(message = "Use property instead", replaceWith = ReplaceWith(expression = "locations"))
  fun locations(): List<Location> = locations

  /**
   * Returns custom attributes associated with this error
   */
  @Deprecated(message = "Use property instead", replaceWith = ReplaceWith(expression = "customAttributes"))
  fun customAttributes(): Map<String, Any?> = customAttributes

  /**
   * Represents the location of the error in the GraphQL operation sent to the server. This location is represented in
   * terms of the line and column number.
   */
  class Location(
      /**
       * Line number of the error location
       */
      val line: Long,

      /**
       * Column number of the error location.
       */
      val column: Long
  ) {

    /**
     * Returns the line number of the error location.
     */
    @Deprecated(message = "Use property instead", replaceWith = ReplaceWith(expression = "line"))
    fun line(): Long = line

    /**
     * Returns the column number of the error location.
     */
    @Deprecated(message = "Use property instead", replaceWith = ReplaceWith(expression = "column"))
    fun column(): Long = column
  }
}
