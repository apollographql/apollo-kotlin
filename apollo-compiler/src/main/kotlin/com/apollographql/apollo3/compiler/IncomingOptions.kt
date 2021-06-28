package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.ast.GQLDocument
import com.apollographql.apollo3.ast.GQLSchemaDefinition
import com.apollographql.apollo3.ast.GQLTypeDefinition
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.ast.apolloDefinitions
import com.apollographql.apollo3.ast.toSchema
import com.apollographql.apollo3.compiler.introspection.toGQLDocument
import java.io.File


/**
 * These are the options that should be the same in all modules.
 */
class IncomingOptions(
    val schema: Schema,
    val schemaPackageName: String,
    val customScalarsMapping: Map<String, String>,
    val codegenModels: String,
    val flattenModels: Boolean,
    val metadataInputObjects: Set<String>,
    val metadataEnums: Set<String>,
    val isFromMetadata: Boolean,
    val metadataFragments: List<MetadataFragment>,
) {
  companion object {
    fun fromMetadata(metadata: ApolloMetadata): IncomingOptions {
      return IncomingOptions(
          schema = metadata.schema!!,
          schemaPackageName = metadata.schemaPackageName,
          customScalarsMapping = metadata.customScalarsMapping,
          codegenModels = metadata.codegenModels,
          flattenModels = metadata.flattenModels,
          metadataInputObjects = metadata.generatedInputObjects,
          metadataEnums = metadata.generatedEnums,
          isFromMetadata = true,
          metadataFragments = metadata.generatedFragments,
      )
    }

    fun fromOptions(
        schemaFiles: Set<File>,
        customScalarsMapping: Map<String, String>,
        codegenModels: String,
        packageNameGenerator: PackageNameGenerator,
        flattenModels: Boolean,
    ): IncomingOptions {
      val schemaDocuments = schemaFiles.map {
        it.toGQLDocument()
      }

      // Locate the mainSchemaDocument. It's the one that contains the operation roots
      val mainSchemaDocuments = schemaDocuments.filter {
        it.definitions.filterIsInstance<GQLSchemaDefinition>().isNotEmpty()
            || it.definitions.filterIsInstance<GQLTypeDefinition>().any { it.name == "Query" }
      }

      check(mainSchemaDocuments.size == 1) {
        "Multiple schemas found:\n${mainSchemaDocuments.map { it.filePath }.joinToString("\n")}\n" +
            "Use different services for different schemas"
      }
      val mainSchemaDocument = mainSchemaDocuments.single()

      val schemaDefinitions = schemaDocuments.flatMap { it.definitions }
      val schemaDocument = GQLDocument(
          definitions = schemaDefinitions + apolloDefinitions(),
          filePath = null
      )

      val schema = schemaDocument.toSchema()

      return IncomingOptions(
          schema = schema,
          schemaPackageName = packageNameGenerator.packageName(mainSchemaDocument.filePath!!),
          customScalarsMapping = customScalarsMapping,
          codegenModels = codegenModels,
          flattenModels = flattenModels,
          metadataInputObjects = emptySet(),
          metadataEnums = emptySet(),
          isFromMetadata = false,
          metadataFragments = emptyList(),
      )
    }
  }
}