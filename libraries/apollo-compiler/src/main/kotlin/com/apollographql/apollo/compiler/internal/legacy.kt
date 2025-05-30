@file:Suppress("DEPRECATION")

package com.apollographql.apollo.compiler.internal

import com.apollographql.apollo.ast.GQLDocument
import com.apollographql.apollo.ast.GQLFragmentDefinition
import com.apollographql.apollo.ast.GQLOperationDefinition
import com.apollographql.apollo.ast.Schema
import com.apollographql.apollo.ast.toSchema
import com.apollographql.apollo.compiler.ApolloCompilerPlugin
import com.apollographql.apollo.compiler.CodeGenerator
import com.apollographql.apollo.compiler.CodegenSchema
import com.apollographql.apollo.compiler.DocumentTransform
import com.apollographql.apollo.compiler.LayoutFactory
import com.apollographql.apollo.compiler.OperationIdsGenerator
import com.apollographql.apollo.compiler.ExecutableDocumentTransform
import com.apollographql.apollo.compiler.SchemaListener
import com.apollographql.apollo.compiler.codegen.SchemaAndOperationsLayout
import com.apollographql.apollo.compiler.operationoutput.OperationDescriptor
import com.apollographql.apollo.compiler.operationoutput.OperationId
import java.io.File
import kotlin.collections.toList

internal class LegacyOperationIdsGenerator(private val plugin: ApolloCompilerPlugin) : OperationIdsGenerator {
  override fun generate(operationDescriptorList: Collection<OperationDescriptor>): List<OperationId> {
    @Suppress("DEPRECATION")
    val operationIds = plugin.operationIds(operationDescriptorList.toList())
    if (operationIds != null) {
      println("Apollo: using ApolloCompiler.operationIds() is deprecated. Please use registry.registerOperationIdsGenerator() from beforeCompilationStep() instead.")
      return operationIds
    }
    return NoList
  }

  companion object {
    internal val NoList: List<OperationId> = emptyList()
  }
}


internal class LegacyLayoutFactory(private val plugin: ApolloCompilerPlugin) : LayoutFactory {
  override fun create(codegenSchema: CodegenSchema): SchemaAndOperationsLayout? {
    val layout = plugin.layout(codegenSchema)
    if (layout != null) {
      println("Apollo: using ApolloCompilerPlugin.layout() is deprecated. Please use registry.registerLayout() from beforeCompilationStep() instead.")
    }
    return layout
  }
}

internal class LegacyExecutableDocumentTransform(private val documentTransform: DocumentTransform): ExecutableDocumentTransform {
  override fun transform(
      schema: Schema,
      document: GQLDocument,
      extraFragmentDefinitions: List<GQLFragmentDefinition>,
  ): GQLDocument {
    return document.copy(
        definitions = document.definitions.map {
          when (it) {
            is GQLFragmentDefinition -> documentTransform.transform(schema, it)
            is GQLOperationDefinition -> documentTransform.transform(schema, it)
            else -> it
          }
        }
    )
  }
}

internal class LegacyExtraCodeGenerator(private val schemaListener: SchemaListener): CodeGenerator {
  override fun generate(schema: GQLDocument, outputDirectory: File) {
    schemaListener.onSchema(schema.toSchema(), outputDirectory)
  }
}