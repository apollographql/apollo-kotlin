package com.apollographql.apollo.compiler

import com.apollographql.apollo.ast.SourceAwareException
import com.apollographql.apollo.compiler.codegen.writeTo
import com.google.common.truth.Truth
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.fail

class MetadataTest {
  private val buildDir = File("build/metadata-test/")
  private val codegenSchemaFile = File(buildDir, "codegenSchema.json")
  private val codegenSchemaOptionsFile = File(buildDir, "codegenSchemaOptions.json")
  private val irOptionsFile = File(buildDir, "irOptions.json")
  private val rootCodegenOptionsFile = File(buildDir, "root-codegenOptions.json")
  private val leafCodegenOptionsFile = File(buildDir, "leaf-codegenOptions.json")
  private val rootCodegenMetadata = File(buildDir, "root-codegen-metadata.json")
  private val rootIrOperationsFile = File(buildDir, "root-ir-operations.json")
  private val leafIrOperationsFile = File(buildDir, "leaf-ir-operations.json")
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


  private fun compileRoot(directory: String) {
    buildCodegenSchemaOptions().writeTo(codegenSchemaOptionsFile)
    buildIrOptions().writeTo(irOptionsFile)
    buildCodegenOptions(packageName = rootPackageName).writeTo(rootCodegenOptionsFile)
    buildCodegenOptions(packageName = leafPackageName).writeTo(leafCodegenOptionsFile)

    ApolloCompiler.buildCodegenSchema(
        schemaFiles = setOf(File("src/test/metadata/schema.graphqls")).toInputFiles(),
        logger = null,
        codegenSchemaOptions = codegenSchemaOptionsFile.toCodegenSchemaOptions(),
        foreignSchemas = emptyList(),
    ).writeTo(codegenSchemaFile)

    ApolloCompiler.buildIrOperations(
        codegenSchema = codegenSchemaFile.toCodegenSchema(),
        executableFiles = setOf(rootGraphQLFile(directory)).toInputFiles(),
        upstreamCodegenModels = emptyList(),
        upstreamFragmentDefinitions = emptyList(),
        documentTransform = null,
        options = irOptionsFile.toIrOptions(),
        logger = null
    ).writeTo(rootIrOperationsFile)

    ApolloCompiler.buildIrOperations(
        codegenSchema = codegenSchemaFile.toCodegenSchema(),
        executableFiles = setOf(leafGraphQLFile(directory)).toInputFiles(),
        upstreamCodegenModels = rootIrOperationsFile.toIrOperations().codegenModels.let { listOf(it) },
        upstreamFragmentDefinitions = rootIrOperationsFile.toIrOperations().fragmentDefinitions,
        documentTransform = null,
        options = irOptionsFile.toIrOptions(),
        logger = null
    ).writeTo(leafIrOperationsFile)

    ApolloCompiler.buildSchemaAndOperationsSourcesFromIr(
        codegenSchema = codegenSchemaFile.toCodegenSchema(),
        irOperations = rootIrOperationsFile.toIrOperations(),
        upstreamCodegenMetadata = emptyList(),
        downstreamUsedCoordinates = leafIrOperationsFile.toIrOperations().usedCoordinates,
        codegenOptions = rootCodegenOptionsFile.toCodegenOptions(),
        layout = null,
        operationManifestFile = null,
        operationOutputGenerator = null,
        irOperationsTransform = null,
        javaOutputTransform = null,
        kotlinOutputTransform = null,
    ).writeTo(rootSourcesDir, true, rootCodegenMetadata)

    ApolloCompiler.buildSchemaAndOperationsSourcesFromIr(
        codegenSchema = codegenSchemaFile.toCodegenSchema(),
        irOperations = leafIrOperationsFile.toIrOperations(),
        upstreamCodegenMetadata = setOf(rootCodegenMetadata).map { it.toCodegenMetadata() },
        downstreamUsedCoordinates = UsedCoordinates(),
        codegenOptions = leafCodegenOptionsFile.toCodegenOptions(),
        layout = null,
        operationManifestFile = null,
        operationOutputGenerator = null,
        irOperationsTransform = null,
        javaOutputTransform = null,
        kotlinOutputTransform = null,
    ).writeTo(leafSourcesDir, true, null)
  }

  private fun compile(directory: String) {
    buildDir.deleteRecursively()
    buildDir.mkdirs()

    compileRoot(directory)
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
