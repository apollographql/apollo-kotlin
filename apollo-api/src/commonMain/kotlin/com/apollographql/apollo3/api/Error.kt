package com.apollographql.apollo3.api

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
  )
}
