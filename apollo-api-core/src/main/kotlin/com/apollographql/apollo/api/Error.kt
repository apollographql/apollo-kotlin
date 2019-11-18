package com.apollographql.apollo.api

/**
 * Represents an error response returned from the GraphQL server
 */
data class Error(
    val message: String?,
    val locations: List<Location>?,
    val customAttributes: Map<String, Any?>?
) {

  /**
   * Returns server error message.
   */
  fun message(): String? = message

  /**
   * Returns the location of the error in the GraphQL operation.
   */
  fun locations(): List<Location> = locations.orEmpty()

  /**
   * Returns custom attributes associated with this error
   */
  fun customAttributes(): Map<String, Any?> = customAttributes.orEmpty()

  /**
   * Represents the location of the error in the GraphQL operation sent to the server. This location is represented in
   * terms of the line and column number.
   */
  data class Location(private val line: Long, private val column: Long) {

    /**
     * Returns the line number of the error location.
     */
    fun line(): Long = line

    /**
     * Returns the column number of the error location.
     */
    fun column(): Long = column
  }
}
