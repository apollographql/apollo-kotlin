package com.apollographql.apollo.compiler

import com.apollographql.apollo.ast.SourceAwareException
import com.apollographql.apollo.compiler.codegen.writeTo
import com.google.common.truth.Truth
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.test.assertFailsWith
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
    CodegenSchemaOptions().writeTo(codegenSchemaOptionsFile)
    buildIrOptions().writeTo(irOptionsFile)
    buildCodegenOptions(packageName = rootPackageName).writeTo(rootCodegenOptionsFile)
    buildCodegenOptions(packageName = leafPackageName).writeTo(leafCodegenOptionsFile)

    ApolloCompiler.buildCodegenSchema(
        schemaFiles = setOf(File("src/test/metadata/schema.graphqls")).toInputFiles(),
        logger = null,
        codegenSchemaOptions = codegenSchemaOptionsFile.toCodegenSchemaOptions(),
        foreignSchemas = emptyList(),
        null
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
        operationIdsGenerator = null,
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
        operationIdsGenerator = null,
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

  private fun prepareFragmentTest(document: String): File {
    buildDir.deleteRecursively()
    buildDir.mkdirs()
    CodegenSchemaOptions().writeTo(codegenSchemaOptionsFile)
    buildIrOptions().writeTo(irOptionsFile)
    ApolloCompiler.buildCodegenSchema(
        schemaFiles = setOf(File("src/test/metadata/schema.graphqls")).toInputFiles(),
        logger = null,
        codegenSchemaOptions = codegenSchemaOptionsFile.toCodegenSchemaOptions(),
        foreignSchemas = emptyList(),
        schemaTransform = null,
    ).writeTo(codegenSchemaFile)
    return File(buildDir, "root.graphql").apply { writeText(document) }
  }

  private fun warningsLogger(warnings: MutableList<String>): ApolloCompiler.Logger {
    return object : ApolloCompiler.Logger {
      override fun debug(message: String) = Unit
      override fun info(message: String) = Unit
      override fun warning(message: String) {
        warnings.add(message)
      }
      override fun error(message: String) = Unit
    }
  }

  private fun buildDeferredIr(
      graphqlFile: File,
      upstreamIrOperations: List<File> = emptyList(),
      logger: ApolloCompiler.Logger = warningsLogger(mutableListOf()),
  ): File {
    val irFile = File(buildDir, "${graphqlFile.nameWithoutExtension}-ir.json")
    EntryPoints.buildIr(
        plugins = emptyList(),
        arguments = emptyMap(),
        logger = logger,
        graphqlFiles = setOf(graphqlFile).toInputFiles(),
        codegenSchemaFiles = setOf(codegenSchemaFile).toInputFiles(),
        upstreamIrOperations = upstreamIrOperations.toInputFiles(),
        irOptionsFile = irOptionsFile,
        irOperationsFile = irFile,
        reportUnusedFragments = false,
    )
    return irFile
  }

  @Test
  fun `IR generation can defer unused fragment diagnostics`() {
    val graphqlFile = prepareFragmentTest("fragment Shared on Query { __typename }")
    buildIrOptions(
        failOnWarnings = true,
        issueSeverity = mapOf("UnusedFragment" to IssueSeverity.Error),
    ).writeTo(irOptionsFile)
    val originalOptions = irOptionsFile.readText()
    val warnings = mutableListOf<String>()

    buildDeferredIr(graphqlFile, logger = warningsLogger(warnings))

    Truth.assertThat(warnings).isEmpty()
    Truth.assertThat(irOptionsFile.readText()).isEqualTo(originalOptions)
  }

  @Test
  fun `existing IR callers still report unused fragments`() {
    val graphqlFile = prepareFragmentTest("fragment Shared on Query { __typename }")
    val warnings = mutableListOf<String>()

    EntryPoints.buildIr(
        plugins = emptyList(),
        arguments = emptyMap(),
        logger = warningsLogger(warnings),
        graphqlFiles = setOf(graphqlFile).toInputFiles(),
        codegenSchemaFiles = setOf(codegenSchemaFile).toInputFiles(),
        upstreamIrOperations = emptyList(),
        irOptionsFile = irOptionsFile,
        irOperationsFile = rootIrOperationsFile,
    )

    Truth.assertThat(warnings).containsExactly("w: build/metadata-test/root.graphql: (1, 1): Apollo: Fragment 'Shared' is not used")
  }

  @Test
  fun `deferring unused fragments preserves other validation errors`() {
    val graphqlFile = prepareFragmentTest("query Root { unknownField }")

    val failure = assertFailsWith<SourceAwareException> {
      buildDeferredIr(graphqlFile)
    }

    Truth.assertThat(failure).hasMessageThat().contains("unknownField")
  }

  @Test
  fun `unused fragment summaries belong only to their defining module`() {
    val rootFile = prepareFragmentTest("""
      fragment Shared on Query { __typename }
      fragment UnusedRoot on Query { __typename }
    """.trimIndent())
    val rootIr = buildDeferredIr(rootFile)
    val childFile = File(buildDir, "child.graphql").apply {
      writeText("""
        query Child { ...Shared }
        fragment UnusedChild on Query { __typename }
      """.trimIndent())
    }
    val childIr = buildDeferredIr(childFile, listOf(rootIr))
    val summaryFile = File(buildDir, "unused-fragments.txt")

    EntryPoints.computeUnusedFragments(
        irOperations = rootIr,
        downstreamIrOperations = listOf(childIr).toInputFiles(),
        downstreamFragmentUsageIsComplete = true,
        outputFile = summaryFile,
    )

    Truth.assertThat(summaryFile.readLines()).containsExactly("UnusedRoot")
  }

  @Test
  fun `unknown downstream visibility is not empty usage`() {
    val rootFile = prepareFragmentTest("fragment Unused on Query { __typename }")
    val rootIr = buildDeferredIr(rootFile)
    val emptyChild = File(buildDir, "child.graphql").apply { writeText("") }
    val childIr = buildDeferredIr(emptyChild, listOf(rootIr))
    val summaryFile = File(buildDir, "unused-fragments.txt")

    EntryPoints.computeUnusedFragments(rootIr, emptyList(), false, summaryFile)
    Truth.assertThat(summaryFile.readText()).isEmpty()

    EntryPoints.computeUnusedFragments(rootIr, listOf(childIr).toInputFiles(), true, summaryFile)
    Truth.assertThat(summaryFile.readLines()).containsExactly("Unused")
  }

  @Test
  fun `summaries preserve syntactic usage through fragments and conditions`() {
    val rootFile = prepareFragmentTest("""
      fragment Orphan on Query { ...Nested }
      fragment Nested on Query { __typename }
      fragment Shared on Query { __typename }
      fragment ConstantSkipped on Query { __typename }
      fragment Unused on Query { __typename }
    """.trimIndent())
    val rootIr = buildDeferredIr(rootFile)
    val middleFile = File(buildDir, "middle.graphql").apply {
      writeText("fragment Wrapper on Query { ...Shared }")
    }
    val middleIr = buildDeferredIr(middleFile, listOf(rootIr))
    val leafFile = File(buildDir, "leaf.graphql").apply {
      writeText("""
        query Leaf(${'$'}include: Boolean!) {
          ...Wrapper @include(if: ${'$'}include)
          ...ConstantSkipped @skip(if: true)
        }
      """.trimIndent())
    }
    val leafIr = buildDeferredIr(leafFile, listOf(rootIr, middleIr))
    val summaryFile = File(buildDir, "unused-fragments.txt")

    EntryPoints.computeUnusedFragments(
        rootIr,
        listOf(middleIr, leafIr).toInputFiles(),
        true,
        summaryFile,
    )

    Truth.assertThat(summaryFile.readLines()).containsExactly("Orphan", "Unused").inOrder()
  }

  @Test
  fun `unrelated downstream changes preserve sorted diagnostic summaries`() {
    val rootFile = prepareFragmentTest("""
      fragment Zulu on Query { __typename }
      fragment Shared on Query { __typename }
      fragment Alpha on Query { __typename }
    """.trimIndent())
    val rootIr = buildDeferredIr(rootFile)
    val childFile = File(buildDir, "child.graphql").apply { writeText("query Child { ...Shared }") }
    val childIr = buildDeferredIr(childFile, listOf(rootIr))
    val summaryFile = File(buildDir, "unused-fragments.txt")

    EntryPoints.computeUnusedFragments(rootIr, listOf(childIr).toInputFiles(), true, summaryFile)
    Truth.assertThat(summaryFile.readText()).isEqualTo("Alpha\nZulu")

    childFile.writeText("query RenamedChild { __typename ...Shared }")
    buildDeferredIr(childFile, listOf(rootIr))
    EntryPoints.computeUnusedFragments(rootIr, listOf(childIr).toInputFiles(), true, summaryFile)
    Truth.assertThat(summaryFile.readText()).isEqualTo("Alpha\nZulu")
  }

  @Test
  fun `deferred warnings preserve source locations and failOnWarnings`() {
    val graphqlFile = prepareFragmentTest("""
      query Root { __typename }
      fragment Unused on Query { __typename }
    """.trimIndent())
    buildIrOptions(failOnWarnings = true).writeTo(irOptionsFile)
    val summaryFile = File(buildDir, "unused-fragments.txt").apply { writeText("Unused") }
    val warnings = mutableListOf<String>()

    val failure = assertFailsWith<IllegalStateException> {
      EntryPoints.checkUnusedFragments(
          unusedFragments = summaryFile,
          graphqlFiles = setOf(graphqlFile).toInputFiles(),
          irOptionsFile = irOptionsFile,
          logger = warningsLogger(warnings),
      )
    }

    Truth.assertThat(warnings).containsExactly("w: build/metadata-test/root.graphql: (2, 1): Apollo: Fragment 'Unused' is not used")
    Truth.assertThat(failure).hasMessageThat().contains("Warnings found and 'failOnWarnings' is true")
  }

  @Test
  fun `deferred diagnostics preserve ignored and error severities`() {
    val graphqlFile = prepareFragmentTest("fragment Unused on Query { __typename }")
    val summaryFile = File(buildDir, "unused-fragments.txt").apply { writeText("Unused") }
    val warnings = mutableListOf<String>()
    buildIrOptions(
        failOnWarnings = true,
        issueSeverity = mapOf("UnusedFragment" to IssueSeverity.Ignore),
    ).writeTo(irOptionsFile)

    EntryPoints.checkUnusedFragments(summaryFile, setOf(graphqlFile).toInputFiles(), irOptionsFile, warningsLogger(warnings))
    Truth.assertThat(warnings).isEmpty()

    buildIrOptions(issueSeverity = mapOf("UnusedFragment" to IssueSeverity.Error)).writeTo(irOptionsFile)
    val failure = assertFailsWith<SourceAwareException> {
      EntryPoints.checkUnusedFragments(summaryFile, setOf(graphqlFile).toInputFiles(), irOptionsFile, warningsLogger(warnings))
    }
    Truth.assertThat(failure).hasMessageThat().contains("Fragment 'Unused' is not used")
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
