package com.apollographql.apollo.compiler

import com.apollographql.apollo.compiler.frontend.GQLFragmentDefinition
import com.apollographql.apollo.compiler.frontend.GraphQLParser
import com.apollographql.apollo.compiler.frontend.Schema
import com.apollographql.apollo.compiler.frontend.toUtf8
import com.squareup.moshi.JsonClass
import java.io.File

data class ApolloMetadata(
    /**
     * Might be null if the schema is coming from upstream
     */
    val schema: Schema?,
    /**
     * The fragments
     */
    val fragments: List<GQLFragmentDefinition>,
    /**
     * The generated input objects, enums
     */
    val types: Set<String>,
    val schemaPackageName: String?,
    /**
     * The module name, for debug
     */
    val moduleName: String,
    val generateKotlinModels: Boolean,
    val pluginVersion: String,
    val customScalarsMapping: Map<String, String>
) {
  @JsonClass(generateAdapter = true)
  internal class JsonMetadata(
    val schema: String?,
    val fragments: String,
    val types: Set<String>,
    val schemaPackageName: String?,
    val moduleName: String,
    val generateKotlinModels: Boolean,
    val pluginVersion: String,
    val customTypesMap: Map<String, String>
  )

  companion object {
    fun List<ApolloMetadata>.merge(): ApolloMetadata? {
      if (isEmpty()) {
        return null
      }
      // ensure a single schema
      val rootMetadataList = filter { it.schema != null }
      check(rootMetadataList.size <= 1) {
        "Apollo: A schema is defined in multiple modules: ${rootMetadataList.map { it.moduleName }.joinToString(", ")}.\n" +
            "There should be only one root module defining the schema, check your dependencies."
      }
      check(rootMetadataList.isNotEmpty()) {
        "Apollo: Cannot find a schema in parent modules. Searched in ${map { it.moduleName }.joinToString(", ")}"
      }
      val rootMetadata = rootMetadataList.first()

      // ensure the same schemaPackageName
      map { it.schemaPackageName }.filterNotNull().distinct().let {
        check(it.size == 1) {
          "Apollo: All modules should have the same schemaPackageName. Found:" + it.joinToString(", ")
        }
      }

      // ensure the same generateKotlinModels
      map { it.generateKotlinModels }.distinct().let {
        check(it.size == 1) {
          "Apollo: All modules should have the same generateKotlinModels. Found:" + it.joinToString(", ")
        }
      }

      // ensure the same pluginVersion
      map { it.pluginVersion }.distinct().let {
        check(it.size == 1) {
          "Apollo: All modules should be generated with the same apollo version. Found:" + it.joinToString(", ")
        }
      }

      // no need to validate distinct fragment names, this will be done later when aggregating the Fragments
      return rootMetadata.copy(
          fragments = flatMap { it.fragments },
          moduleName = "*",
          types = flatMap { it.types }.toSet(),
      )
    }

    fun readFrom(file: File): ApolloMetadata {
      val serializedMetadata = file.fromJson<JsonMetadata>()
      return with(serializedMetadata) {
        ApolloMetadata(
            schema = schema?.let { GraphQLParser.parseSchema(it) },
            fragments = GraphQLParser.parseDocument(fragments).orThrow().definitions.map { it as GQLFragmentDefinition },
            types = types,
            schemaPackageName = schemaPackageName,
            moduleName = moduleName,
            generateKotlinModels = generateKotlinModels,
            pluginVersion = pluginVersion,
            customScalarsMapping = customTypesMap
        )
      }
    }
  }

  fun writeTo(file: File) {
    val serializedMetadata = JsonMetadata(
        schema = schema?.toDocument()?.toUtf8(),
        fragments = fragments.map { it.toUtf8() }.joinToString("\n"),
        types = types,
        schemaPackageName = schemaPackageName,
        moduleName = moduleName,
        generateKotlinModels = generateKotlinModels,
        pluginVersion = pluginVersion,
        customTypesMap = customScalarsMapping
    )
    serializedMetadata.toJson(file)
  }

}
