package com.apollographql.apollo.gradle.api

import org.gradle.api.Project

/**
 * @Deprecated
 *
 * This class is only there for backward compatibility reasons with the old groovy plugin
 */
open class ApolloSourceSetExtension(project: Project) {
  val schemaFile = project.objects.property(String::class.java)
  val exclude = project.objects.listProperty(String::class.java)

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
