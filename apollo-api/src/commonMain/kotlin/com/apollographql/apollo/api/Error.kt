package com.apollographql.apollo.api

import kotlin.js.JsName

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
  @JsName("getMessage")
  fun message(): String? = message

  /**
   * Returns the location of the error in the GraphQL operation.
   */
  @Deprecated(message = "Use property instead", replaceWith = ReplaceWith(expression = "locations"))
  @JsName("getLocations")
  fun locations(): List<Location> = locations

  /**
   * Returns custom attributes associated with this error
   */
  @Deprecated(message = "Use property instead", replaceWith = ReplaceWith(expression = "customAttributes"))
  @JsName("getCustomAttributes")
  fun customAttributes(): Map<String, Any?> = customAttributes

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Error) return false

    if (message != other.message) return false
    if (locations != other.locations) return false
    if (customAttributes != other.customAttributes) return false

    return true
  }

  override fun hashCode(): Int {
    var result = message.hashCode()
    result = 31 * result + locations.hashCode()
    result = 31 * result + customAttributes.hashCode()
    return result
  }

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
    @JsName("getLine")
    fun line(): Long = line

    /**
     * Returns the column number of the error location.
     */
    @Deprecated(message = "Use property instead", replaceWith = ReplaceWith(expression = "column"))
    @JsName("getColumn")
    fun column(): Long = column

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other !is Location) return false

      if (line != other.line) return false
      if (column != other.column) return false

      return true
    }

    override fun hashCode(): Int {
      var result = line.hashCode()
      result = 31 * result + column.hashCode()
      return result
    }
  }
}
