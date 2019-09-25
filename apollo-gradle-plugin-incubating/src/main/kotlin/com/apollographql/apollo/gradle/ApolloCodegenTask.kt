package com.apollographql.apollo.gradle

import com.apollographql.apollo.compiler.GraphQLCompiler
import com.apollographql.apollo.compiler.NullableValueType
import com.apollographql.apollo.compiler.PackageNameProvider
import com.apollographql.apollo.compiler.parser.GraphQLDocumentParser
import com.apollographql.apollo.compiler.parser.Schema
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.*
import java.io.File

@CacheableTask
open class ApolloCodegenTask : SourceTask() {
  @get:Input
  var schemaFile: File? = null

  @get:Input
  lateinit var rootPackageName: String

  @get:Input
  lateinit var schemaPackageName: String

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

  @Optional
  @get:Input
  var transformedQueriesOutputDir: File? = null

  @InputFiles
  @SkipWhenEmpty
  @PathSensitive(PathSensitivity.RELATIVE)
  override fun getSource(): FileTree {
    return super.getSource()
  }

  @Internal
  lateinit var graphqlFiles: List<File>

  @TaskAction
  fun taskAction() {
    val nullableValueTypeEnum = NullableValueType.values().find { it.value == nullableValueType }

    if (schemaFile == null || !schemaFile!!.exists()) {
      return
    }

    if (graphqlFiles.isEmpty()) {
      return
    }

    outputDir.delete()

    val schema = Schema.invoke(schemaFile!!)

    val packageNameProvider = PackageNameProvider(
        schemaPackageName = schemaPackageName,
        rootPackageName = rootPackageName,
        outputPackageName = null
    )

    val codeGenerationIR = GraphQLDocumentParser(schema, packageNameProvider).parse(graphqlFiles)
    val args = GraphQLCompiler.Arguments(
        ir = codeGenerationIR,
        outputDir = outputDir,
        customTypeMap = customTypeMapping,
        nullableValueType = nullableValueTypeEnum ?: NullableValueType.ANNOTATED,
        useSemanticNaming = useSemanticNaming,
        generateModelBuilder = generateModelBuilder,
        useJavaBeansSemanticNaming = useJavaBeansSemanticNaming,
        suppressRawTypesWarning = suppressRawTypesWarning,
        generateKotlinModels = generateKotlinModels,
        generateVisitorForPolymorphicDatatypes = generateVisitorForPolymorphicDatatypes,
        packageNameProvider = packageNameProvider,
        transformedQueriesOutputDir = transformedQueriesOutputDir
    )
    GraphQLCompiler().write(args)
  }
}