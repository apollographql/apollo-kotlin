package com.apollographql.apollo.gradle.kotlin

import com.apollographql.apollo.compiler.GraphQLCompiler
import com.apollographql.apollo.compiler.NullableValueType
import com.apollographql.apollo.compiler.formatPackageName
import com.apollographql.apollo.compiler.parser.GraphQLDocumentParser
import com.apollographql.apollo.compiler.parser.Schema
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.*
import java.io.File

open class ApolloCodegenTask : SourceTask() {
  @get:Input
  var schemaFile: File? = null

  @Optional
  @get:Input
  var outputPackageName: String? = null

  @get:OutputDirectory
  lateinit var outputDir: File

  @get:Input
  lateinit var customTypeMapping: Map<String, String>

  @get:Input
  lateinit var nullableValueType: String

  @get:Input
  var useSemanticNaming: Boolean = false

  @get:Input
  var generateModelBuilder: Boolean = false

  @get:Input
  var useJavaBeansSemanticNaming: Boolean = false

  @get:Input
  var suppressRawTypesWarning: Boolean = false

  @get:Input
  var generateKotlinModels: Boolean = false

  @get:Input
  var generateVisitorForPolymorphicDatatypes: Boolean = false

  @InputFiles
  @SkipWhenEmpty
  @PathSensitive(PathSensitivity.RELATIVE)
  override fun getSource(): FileTree {
    return super.getSource()
  }

  @Internal lateinit var graphqlFiles: List<File>

  @TaskAction
  fun taskAction() {
    val nullableValueTypeEnum = NullableValueType.values().find { it.value == nullableValueType }

    outputDir.delete()

    if (schemaFile == null || !schemaFile!!.exists()) {
      return
    }

    if (graphqlFiles.isEmpty()) {
      return
    }

    val schema = Schema.invoke(schemaFile!!)
    val codeGenerationIR = GraphQLDocumentParser(schema).parse(graphqlFiles)

    val args = GraphQLCompiler.Arguments(
        null,
        codeGenerationIR,
        outputDir,
        customTypeMapping,
        nullableValueTypeEnum ?: NullableValueType.ANNOTATED,
        useSemanticNaming,
        generateModelBuilder,
        useJavaBeansSemanticNaming,
        outputPackageName,
        suppressRawTypesWarning,
        generateKotlinModels,
        generateVisitorForPolymorphicDatatypes
    )
    GraphQLCompiler().write(args)
  }
}