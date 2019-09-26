package com.apollographql.apollo.gradle

import groovy.lang.Closure
import org.gradle.api.Action

class Service(val name: String) {
  /**
   * Place where the schema.json file is.
   * This path is relative to the current project directory.
   * schemaFilePath can point outside of src/{foo}/graphql but in that case, you'll want
   * to define rootPackageName else fragments/types will be stored at the root of the namespace.
   * By default, the plugin will look for a schema.json file in src/{foo}/graphql
   */
  var schemaFilePath: String? = null

  /**
   * Place where the graphql files are searched.
   * This path is relative to the current sourceSet (e.g src/{foo}/graphql/{sourceFolderPath})
   * By default, this is the directory where the schema.json is stored or "." if the schema is outside.
   * You need to define this if you have two or more services whose schema is stored outside src/{foo}/graphql.
   * If not, you can certainly omit it.
   */
  var sourceFolderPath: String? = null

  var rootPackageName: String? = null

  /**
   * list of pattern of files to exclude
   */
  var exclude: List<String>? = null

  var introspection: Introspection? = null

  fun introspection(closure: Closure<Introspection>) {
    val introspection = Introspection()
    closure.delegate = introspection
    closure.resolveStrategy = Closure.DELEGATE_FIRST
    closure.call()

    introspection(introspection)
  }

  fun introspection(action: Action<Introspection>) {
    val introspection = Introspection()
    action.execute(introspection)

    introspection(introspection)
  }

  fun introspection(introspection: Introspection) {
    if (introspection.endpointUrl == null) {
      throw IllegalArgumentException("introspection must have a url")
    }
    if (this.introspection != null) {
      throw IllegalArgumentException("there must be only one introspection block")
    }
    this.introspection = introspection
  }
}