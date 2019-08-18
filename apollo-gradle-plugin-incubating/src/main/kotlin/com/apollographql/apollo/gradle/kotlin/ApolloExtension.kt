package com.apollographql.apollo.gradle.kotlin

import com.apollographql.apollo.compiler.NullableValueType
import org.gradle.api.Action

open class ApolloExtension {

  var nullableValueType = NullableValueType.ANNOTATED.value
  var useSemanticNaming = true
  var generateModelBuilder = false
  var useJavaBeansSemanticNaming = false
  var suppressRawTypesWarning = false
  var generateKotlinModels = false
  var generateVisitorForPolymorphicDatatypes = false
  /**
   * Deprecated, use services instead
   */
  var schemaFilePath: String? = null
  /**
   * Deprecated, use services instead
   */
  var outputPackageName: String? = null
  var customTypeMapping: Map<String, String> = emptyMap()

  var services = Services()
  var sourceSet = SourceSet()

  /**
   * Deprecated
   */
  fun sourceSet(action: Action<SourceSet>) {
    action.execute(sourceSet)
  }

  fun services(action: Action<Services>) {
    action.execute(services)
  }

  class Services {
    val all = mutableListOf<Service>()

    fun create(name: String, action: Action<Service>) {
      val service = Service(name)
      action.execute(service)
      all.add(service)
    }
  }

  class Service(val name: String) {
    fun validate() {
      if (!::schemaFilePath.isInitialized) {
        throw java.lang.IllegalArgumentException("""
        |You need to specify the location of your schemaFilePath:
        |
        |apollo {
        |  service(\"${name}\") {
        |    schemaFilePath = "graphql/schema.json"
        |  }
        |}
      """.trimMargin())
      }
    }

    /**
     * path to the schema file, relative to the sourceSets.
     *
     * e.g com/example/schema.json will resolve as src/main/graphql/com/example/schema.json
     *
     * If the schemaFilePath is present in multiple sourceSets, the first one will be used, with the order defined by the android plugin
     */
    lateinit var schemaFilePath: String

    /**
     * path to the .graphql and .gql files, relative to the source set
     *
     * e.g com/example/ will search for files in src/main/graphql/com/example/ *
     *
     * Defaults to the the directory of the schemaFilePath
     */
    var graphqlFilesFolder: String? = null

    /**
     * list of pattern of files to exclude as in @{link kotlin.text.Regex}
     *
     * Defaults to null
     */
    var exclude: List<String>? = null

    /**
     * List of patterns of files to include as in @{link kotlin.text.Regex}
     *
     * Defaults to ".*\.gql, .*\.graphql"
     */
    var include: List<String>? = null

    /**
     * The package to use when generating models
     *
     * Defaults to graphqlFilesFolder
     *
     * e.g if graphqlFilesFolder is "com/example/", packageName will be "com.example"
     */
    var packageName: String? = null
  }

  class SourceSet {
    var schemaFile = ""
    var exclude: Any = emptyList<String>()
  }

  fun validate() {
    services.all.forEach {
      it.validate()
    }
  }
}