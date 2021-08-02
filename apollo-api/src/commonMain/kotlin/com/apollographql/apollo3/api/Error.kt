package com.apollographql.apollo3.api

/**
 * Represents an error response returned from the GraphQL server
 * See https://spec.graphql.org/draft/#sec-Errors.Error-result-format
 */
class Error(
    /**
     * Server error message
     */
    val message: String,

    /**
     * Locations of the errors in the GraphQL operation
     * It may be null if the location cannot be determined
     */
    val locations: List<Location>? = null,

    /**
     * If this error comes from a field, the path of the field where the error happened.
     * Values in the list can be either Strings or Int
     * Can be null if the error doesn't come from a field, like validation errors.
     */
    val path: List<Any>? = null,

    /**
     * Extensions if any.
     */
    val extensions: Map<String, Any?>? = null,
) {

  override fun toString(): String {
    return "Error(message = $message, locations = $locations, path=$path, extensions = $extensions)"
  }

  /**
   * Represents the location of the error in the GraphQL operation sent to the server. This location is represented in
   * terms of the line and column number.
   */
  class Location(
      /**
       * Line number of the error location
       */
      val line: Int,

      /**
       * Column number of the error location.
       */
      val column: Int,
  ) {
    override fun toString(): String {
      return "Location(line = $line, column = $column)"
    }
  }
}
