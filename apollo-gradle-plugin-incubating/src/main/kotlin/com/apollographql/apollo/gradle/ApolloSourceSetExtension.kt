package com.apollographql.apollo.gradle

/**
 * @Deprecated
 *
 * This class is only there for backward compatibility reasons with the old groovy plugin
 */
open class ApolloSourceSetExtension {
  @JvmField
  var schemaFile: String? = null
  @JvmField
  var exclude: List<String>? = null

  fun setSchemaFile(schemaFile: String) {
    this.schemaFile = schemaFile
  }

  fun setExclude(exclude: List<String>) {
    this.exclude = exclude
  }

  fun setExclude(exclude: String) {
    this.exclude = listOf(exclude)
  }
}
