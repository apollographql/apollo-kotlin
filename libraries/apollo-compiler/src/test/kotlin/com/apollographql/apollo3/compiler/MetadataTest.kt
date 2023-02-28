package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.ast.SourceAwareException
import com.apollographql.apollo3.compiler.ir.IrOperations
import com.apollographql.apollo3.compiler.ir.toIrOperations
import com.apollographql.apollo3.compiler.ir.writeTo
import com.google.common.truth.Truth
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.fail

class MetadataTest {
  private val buildDir = File("build/metadata-test/")
  private val codegenSchema = File(buildDir, "codegenSchema.json")
  private val rootIr = File(buildDir, "root-ir-operations.json")
  private val leafIr = File(buildDir, "leaf-ir-operations.json")
  private val rootSourcesDir = File(buildDir, "root/sources")
  private val leafSourcesDir = File(buildDir, "leaf/sources")

  private fun rootGraphQLFile(directory: String): File {
    return File("src/test/metadata/$directory/root.graphql")
  }

  private fun leafGraphQLFile(directory: String): File {
    return File("src/test/metadata/$directory/leaf.graphql")
  }

  private val rootPackageName = "root"
  private val leafPackageName = "leaf"

  private fun withBuildDir(block: () -> Unit) {
    buildDir.deleteRecursively()
    buildDir.mkdirs()
    block()
  }

  private fun buildCodegenSchema(): CodegenSchema {
    val schemaFile = File("src/test/metadata/schema.graphqls")
    val codegenSchema = ApolloCompiler.buildCodegenSchema(
        schemaFiles = listOf(schemaFile),
        logger = defaultLogger,
        packageNameGenerator = PackageNameGenerator.Flat(rootPackageName),
        scalarMapping = emptyMap(),
        codegenModels = defaultCodegenModels,
        targetLanguage = defaultTargetLanguage,
        generateDataBuilders = false
    )

    this.codegenSchema.let {
      it.parentFile.mkdirs()
      codegenSchema.writeTo(it)
    }

    return this.codegenSchema.toCodegenSchema()
  }

  @Test
  fun codegenSchemaIsSerializable() = withBuildDir {
    buildCodegenSchema()
  }

  private fun buildIrOperations(
      codegenSchema: CodegenSchema,
      executableFile: File,
      outputFile: File,
      upstreamIr: List<IrOperations>,
      alwaysGenerateTypesMatching: Set<String>,
  ): IrOperations {

    val irOptions = IrOptions(
        executableFiles = setOf(executableFile),
        codegenSchema = codegenSchema,
        incomingFragments = upstreamIr.flatMap { it.fragmentDefinitions },
        fieldsOnDisjointTypesMustMerge = defaultFieldsOnDisjointTypesMustMerge,
        decapitalizeFields = defaultDecapitalizeFields,
        flattenModels = defaultFlattenModels,
        warnOnDeprecatedUsages = false,
        failOnWarnings = true,
        logger = defaultLogger,
        addTypename = defaultAddTypename,
        generateOptionalOperationVariables = defaultGenerateOptionalOperationVariables,
        alwaysGenerateTypesMatching = alwaysGenerateTypesMatching
    )
    val irOperations = ApolloCompiler.buildIrOperations(irOptions)

    outputFile.let {
      it.parentFile.mkdirs()
      irOperations.writeTo(outputFile)
    }
    return outputFile.toIrOperations()
  }

  @Test
  fun irOperationsIsSerializable() = withBuildDir {
    buildIrOperations(
        codegenSchema = buildCodegenSchema(),
        executableFile = rootGraphQLFile("simple"),
        outputFile = rootIr,
        upstreamIr = emptyList(),
        alwaysGenerateTypesMatching = emptySet()
    )

    rootIr.toIrOperations()
  }

  private fun compile(directory: String) {
    val codegenSchema = buildCodegenSchema()

    val rootIrOperations = buildIrOperations(
        codegenSchema = buildCodegenSchema(),
        executableFile = rootGraphQLFile(directory),
        outputFile = rootIr,
        upstreamIr = emptyList(),
        alwaysGenerateTypesMatching = emptySet()
    )

    val leafIrOperations = buildIrOperations(
        codegenSchema = buildCodegenSchema(),
        executableFile = leafGraphQLFile(directory),
        outputFile = leafIr,
        upstreamIr = listOf(rootIrOperations),
        alwaysGenerateTypesMatching = emptySet()
    )

    val rootIrSchema = ApolloCompiler.buildIrSchema(
        codegenSchema = codegenSchema,
        usedFields = leafIrOperations.usedFields.mergeWith(rootIrOperations.usedFields),
        incomingTypes = emptySet()
    )

    val rootOperationOutput = ApolloCompiler.buildOperationOutput(
        rootIrOperations,
        defaultOperationOutputGenerator,
        null
    )

    val rootCommonCodegenOptions = CommonCodegenOptions(
        codegenSchema = codegenSchema,
        ir = rootIrOperations,
        irSchema = rootIrSchema,
        operationOutput = rootOperationOutput,
        incomingCodegenMetadata = emptyList(),
        outputDir = rootSourcesDir,
        packageNameGenerator = PackageNameGenerator.Flat(rootPackageName),
        useSemanticNaming = defaultUseSemanticNaming,
        generateFragmentImplementations = defaultGenerateFragmentImplementations,
        generateResponseFields = defaultGenerateResponseFields,
        generateQueryDocument = defaultGenerateQueryDocument,
        generateSchema = defaultGenerateSchema,
        generatedSchemaName = defaultGeneratedSchemaName
    )

    val kotlinCodegenOptions = KotlinCodegenOptions(
        languageVersion = defaultTargetLanguage,
        sealedClassesForEnumsMatching = defaultSealedClassesForEnumsMatching,
        generateAsInternal = defaultGenerateAsInternal,
        generateFilterNotNull = defaultGenerateFilterNotNull,
        compilerKotlinHooks = defaultCompilerKotlinHooks,
        addJvmOverloads = defaultAddJvmOverloads,
        requiresOptInAnnotation = defaultRequiresOptInAnnotation
    )

    val codegenMetadata = ApolloCompiler.writeKotlin(
        commonCodegenOptions = rootCommonCodegenOptions,
        kotlinCodegenOptions = kotlinCodegenOptions
    )

    val leafOperationOutput = ApolloCompiler.buildOperationOutput(
        leafIrOperations,
        defaultOperationOutputGenerator,
        null
    )
    val leafIrSchema = ApolloCompiler.buildIrSchema(
        codegenSchema = codegenSchema,
        usedFields = leafIrOperations.usedFields,
        incomingTypes = codegenMetadata.schemaTypes()
    )

    val leafCommonCodegenOptions = rootCommonCodegenOptions.copy(
        ir = leafIrOperations,
        irSchema = leafIrSchema,
        incomingCodegenMetadata = listOf(codegenMetadata),
        operationOutput = leafOperationOutput,
        outputDir = leafSourcesDir,
        packageNameGenerator = PackageNameGenerator.Flat(leafPackageName),
    )

    ApolloCompiler.writeKotlin(
        commonCodegenOptions = leafCommonCodegenOptions,
        kotlinCodegenOptions = kotlinCodegenOptions
    )
  }

  @Test
  fun simple() {
    compile("simple")

    assertTrue(rootSourcesDir.resolve("root/type").exists())
    assertTrue(rootSourcesDir.resolve("root/type/Character.kt").exists())
    assertTrue(rootSourcesDir.resolve("root/fragment/CharacterFragment.kt").exists())
    assertFalse(leafSourcesDir.resolve("leaf/type").exists())
  }

  @Test
  fun `fragment-multiple`() {
    compile("fragment-multiple")
  }

  @Test
  fun `fragment-nameclash-error`() {
    try {
      compile("fragment-nameclash-error")
      fail("Parsing the fragment should have failed")
    } catch (e: SourceAwareException) {
      Truth.assertThat(e.message).contains("Fragment CharacterFragment is already defined")
    }
  }

  @Test
  fun `fragment-variable-error`() {
    try {
      compile("fragment-variable-error")
      fail("Parsing the fragment should have failed")
    } catch (e: SourceAwareException) {
      Truth.assertThat(e.message).contains("Variable `first` is not defined by operation `GetCharacter`")
    }
  }
}
