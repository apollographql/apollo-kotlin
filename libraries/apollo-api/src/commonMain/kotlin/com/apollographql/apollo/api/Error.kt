package com.apollographql.apollo.api

import com.apollographql.apollo.annotations.ApolloDeprecatedSince

/**
 * Represents an error response returned from the GraphQL server
 * See https://spec.graphql.org/draft/#sec-Errors.Error-result-format
 */
class Error
@Deprecated("Use Error.Builder instead", ReplaceWith("Builder(message = message).locations(locations).path(path).extensions(extensions)"))
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
constructor(
    /**
     * Server error message
     */
    val message: String,

    /**
     * Locations of the errors in the GraphQL operation
     * It may be null if the location cannot be determined
     */
    val locations: List<Location>?,

    /**
     * If this error comes from a field, the path of the field where the error happened.
     * Values in the list can be either Strings or Int
     * Can be null if the error doesn't come from a field, like validation errors.
     */
    val path: List<Any>?,

    /**
     * Extensions if any.
     */
    val extensions: Map<String, Any?>?,

    /**
     * Other non-standard fields (discouraged but allowed in the spec), if any.
     */
    val nonStandardFields: Map<String, Any?>?,
) {
  class Builder(val message: String) {
    var locations: List<Location>? = null
    var path: List<Any>? = null
    val extensions = mutableMapOf<String, Any?>()

    fun locations(locations: List<Location>) = apply {
      this.locations = locations
    }

    fun path(path: List<Any>) = apply {
      this.path = path
    }

    fun putExtension(name: String, value: Any?) = apply {
      this.extensions.put(name, value)
    }

    fun build(): Error {
      @Suppress("DEPRECATION")
      return Error(
          message = message,
          locations = locations,
          path = path,
          extensions = extensions,
          nonStandardFields = null
      )
    }
  }

  override fun toString(): String {
    return "Error(message = $message, locations = $locations, path=$path, extensions = $extensions, nonStandardFields = $nonStandardFields)"
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
