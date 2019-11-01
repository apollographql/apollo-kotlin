package com.apollographql.apollo.gradle.api

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

/**
 * @Deprecated
 *
 * This class is only there for backward compatibility reasons with the old groovy plugin
 */
@Deprecated("please use services instead")
open class ApolloSourceSetExtension @Inject constructor(objects: ObjectFactory) {
  val schemaFile = objects.property(String::class.java)
  val exclude = objects.listProperty(String::class.java)

  fun setSchemaFile(schemaFile: String) {
    this.schemaFile.set(schemaFile)
  }

  fun setExclude(exclude: List<String>) {
    this.exclude.set(exclude)
  }

  fun setExclude(exclude: String) {
    this.exclude.set(listOf(exclude))
  }
}
