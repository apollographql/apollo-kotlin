package com.apollographql.apollo.compiler

import com.apollographql.apollo.ast.GQLFragmentDefinition
import com.apollographql.apollo.ast.SourceAwareException
import com.apollographql.apollo.compiler.codegen.writeTo
import com.apollographql.apollo.compiler.ir.IrOperations
import com.apollographql.apollo.compiler.ir.computeUsedFragmentNames
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

  /**
   * Writes the codegen schema once so that [buildIrOperationsForModule] can be called repeatedly to build a chain
   * (or any other topology) of modules by hand, without going through the fixed root/leaf shape of [compile].
   */
  private fun prepareSchemaForChain() {
    buildDir.deleteRecursively()
    buildDir.mkdirs()

    CodegenSchemaOptions().writeTo(codegenSchemaOptionsFile)
    buildIrOptions().writeTo(irOptionsFile)

    ApolloCompiler.buildCodegenSchema(
        schemaFiles = setOf(File("src/test/metadata/schema.graphqls")).toInputFiles(),
        logger = null,
        codegenSchemaOptions = codegenSchemaOptionsFile.toCodegenSchemaOptions(),
        foreignSchemas = emptyList(),
        null
    ).writeTo(codegenSchemaFile)
  }

  private fun buildIrOperationsForModule(
      directory: String,
      fileName: String,
      upstreamFragmentDefinitions: List<GQLFragmentDefinition>,
  ): IrOperations {
    return ApolloCompiler.buildIrOperations(
        codegenSchema = codegenSchemaFile.toCodegenSchema(),
        executableFiles = setOf(File("src/test/metadata/$directory/$fileName.graphql")).toInputFiles(),
        upstreamCodegenModels = emptyList(),
        upstreamFragmentDefinitions = upstreamFragmentDefinitions,
        documentTransform = null,
        options = irOptionsFile.toIrOptions(),
        logger = null
    )
  }

  private fun newWarningsLogger(): Pair<MutableList<String>, ApolloCompiler.Logger> {
    val warnings = mutableListOf<String>()
    val logger = object : ApolloCompiler.Logger {
      override fun debug(message: String) = Unit
      override fun info(message: String) = Unit
      override fun warning(message: String) {
        warnings.add(message)
      }
      override fun error(message: String) = Unit
    }
    return warnings to logger
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
  fun `unused fragment warnings fail before multi-module sources or metadata are written`() {
    prepareSchemaForChain()
    val rootIr = buildIrOperationsForModule("fragment-unused", "root", emptyList())
    val leafIr = buildIrOperationsForModule("fragment-unused", "leaf", rootIr.fragmentDefinitions)
    rootIr.writeTo(rootIrOperationsFile)
    buildIrOptions(failOnWarnings = true).writeTo(irOptionsFile)
    buildCodegenOptions(packageName = rootPackageName).writeTo(rootCodegenOptionsFile)
    val usedCoordinatesFile = File(buildDir, "used-coordinates.json")
    leafIr.usedCoordinates.writeTo(usedCoordinatesFile)
    val usedFragmentNamesFile = File(buildDir, "used-fragment-names.json")
    computeUsedFragmentNames(listOf(leafIr)).toUsedFragmentNames().writeTo(usedFragmentNamesFile)
    val (warnings, logger) = newWarningsLogger()

    val exception = assertFailsWith<IllegalStateException> {
      EntryPoints.buildSourcesFromIr(
          plugins = emptyList(),
          arguments = emptyMap(),
          logger = logger,
          codegenSchemas = setOf(codegenSchemaFile).toInputFiles(),
          upstreamMetadata = emptyList(),
          irOperations = rootIrOperationsFile,
          downstreamUsedCoordinates = usedCoordinatesFile,
          downstreamUsedFragmentNames = usedFragmentNamesFile,
          downstreamFragmentUsageIsComplete = true,
          codegenOptions = rootCodegenOptionsFile,
          irOptions = irOptionsFile,
          operationManifest = null,
          outputDirectory = rootSourcesDir,
          metadataOutput = rootCodegenMetadata,
      )
    }

    Truth.assertThat(exception).hasMessageThat().contains("Warnings found and 'failOnWarnings' is true")
    Truth.assertThat(warnings).hasSize(1)
    Truth.assertThat(warnings.single()).contains("Fragment 'GloballyUnusedFragment' is not used")
    assertFalse(rootSourcesDir.exists())
    assertFalse(rootCodegenMetadata.exists())
  }

  @Test
  fun `checkUnusedFragments only reports fragments unreachable from downstream modules`() {
    compile("fragment-unused")

    val rootIrOperations = rootIrOperationsFile.toIrOperations()
    val leafIrOperations = leafIrOperationsFile.toIrOperations()

    val (warnings, logger) = newWarningsLogger()

    // Leaf spreads "RootUsedFragment" (defined in root), which in turn spreads "NestedFragment" (also in root).
    // Both are only reachable transitively through leaf's spread + root's own fragment-to-fragment chain.
    val downstreamUsedFragmentNames = computeUsedFragmentNames(listOf(leafIrOperations)).toUsedFragmentNames()

    ApolloCompiler.checkUnusedFragments(
        irOperations = rootIrOperations,
        downstreamUsedFragmentNames = downstreamUsedFragmentNames,
        options = irOptionsFile.toIrOptions(),
        logger = logger,
    )

    Truth.assertThat(warnings).containsExactly("w: null: (1, 1): Apollo: Fragment 'GloballyUnusedFragment' is not used")
  }

  @Test
  fun `checkUnusedFragments on a single module without downstream data only reports locally unused fragments`() {
    // No leaf/downstream module involved at all: this is the plain single-module case (e.g. a project with no
    // 'dependsOn'/multi-module setup), where checkUnusedFragments is called with the default, empty
    // downstreamUsedFragmentNames.
    CodegenSchemaOptions().writeTo(codegenSchemaOptionsFile)
    buildIrOptions().writeTo(irOptionsFile)

    ApolloCompiler.buildCodegenSchema(
        schemaFiles = setOf(File("src/test/metadata/schema.graphqls")).toInputFiles(),
        logger = null,
        codegenSchemaOptions = codegenSchemaOptionsFile.toCodegenSchemaOptions(),
        foreignSchemas = emptyList(),
        null
    ).writeTo(codegenSchemaFile)

    val irOperations = ApolloCompiler.buildIrOperations(
        codegenSchema = codegenSchemaFile.toCodegenSchema(),
        executableFiles = setOf(File("src/test/metadata/single-module-fragment-unused/root.graphql")).toInputFiles(),
        upstreamCodegenModels = emptyList(),
        upstreamFragmentDefinitions = emptyList(),
        documentTransform = null,
        options = irOptionsFile.toIrOptions(),
        logger = null
    )

    val (warnings, logger) = newWarningsLogger()

    ApolloCompiler.checkUnusedFragments(
        irOperations = irOperations,
        options = irOptionsFile.toIrOptions(),
        logger = logger,
    )

    Truth.assertThat(warnings).containsExactly("w: src/test/metadata/single-module-fragment-unused/root.graphql: (11, 1): Apollo: Fragment 'UnusedFragment' is not used")
  }

  @Test
  fun `checkUnusedFragments counts constant and variable guarded spreads as used`() {
    prepareSchemaForChain()

    val irOperations = buildIrOperationsForModule("fragment-unused-constant-skip", "root", emptyList())

    val (warnings, logger) = newWarningsLogger()
    ApolloCompiler.checkUnusedFragments(
        irOperations = irOperations,
        options = irOptionsFile.toIrOptions(),
        logger = logger,
    )

    Truth.assertThat(warnings).containsExactly(
        "w: src/test/metadata/fragment-unused-constant-skip/root.graphql: (16, 1): Apollo: Fragment 'UnreferencedFragment' is not used"
    )
  }

  @Test
  fun `computeUsedFragmentNames propagates usage through an intermediate module's own fragment (3-tier chain)`() {
    // Regression test for a confirmed false positive: a fragment defined in an intermediate module ("middle")
    // that itself spreads an upstream ("root") fragment must be recognized as making the upstream fragment used,
    // even though "middle" itself has no operations of its own and only "leaf" spreads "middle"'s fragment.
    prepareSchemaForChain()

    val rootIr = buildIrOperationsForModule("fragment-unused-3tier", "root", emptyList())
    val middleIr = buildIrOperationsForModule("fragment-unused-3tier", "middle", rootIr.fragmentDefinitions)
    val leafIr = buildIrOperationsForModule("fragment-unused-3tier", "leaf", rootIr.fragmentDefinitions + middleIr.fragmentDefinitions)

    val usedFragmentNames = computeUsedFragmentNames(listOf(middleIr, leafIr))

    Truth.assertThat(usedFragmentNames).containsExactly("MiddleFragment", "RootFragment")

    val (warnings, logger) = newWarningsLogger()
    ApolloCompiler.checkUnusedFragments(
        irOperations = rootIr,
        downstreamUsedFragmentNames = usedFragmentNames.toUsedFragmentNames(),
        options = irOptionsFile.toIrOptions(),
        logger = logger,
    )

    Truth.assertThat(warnings).containsExactly(
        "w: src/test/metadata/fragment-unused-3tier/root.graphql: (5, 1): Apollo: Fragment 'RootUnusedFragment' is not used"
    )
  }

  @Test
  fun `computeUsedFragmentNames propagates usage through two levels of intermediate-owned fragments (4-tier chain)`() {
    // Same idea as the 3-tier case, but one level deeper: "leaf" only spreads "midB"'s fragment, "midB"'s
    // fragment only spreads "midA"'s fragment, and "midA"'s fragment only spreads "root"'s fragment. All three
    // must be discovered as used.
    prepareSchemaForChain()

    val rootIr = buildIrOperationsForModule("fragment-unused-4tier", "root", emptyList())
    val midAIr = buildIrOperationsForModule("fragment-unused-4tier", "mida", rootIr.fragmentDefinitions)
    val midBIr = buildIrOperationsForModule("fragment-unused-4tier", "midb", rootIr.fragmentDefinitions + midAIr.fragmentDefinitions)
    val leafIr = buildIrOperationsForModule(
        "fragment-unused-4tier", "leaf", rootIr.fragmentDefinitions + midAIr.fragmentDefinitions + midBIr.fragmentDefinitions
    )

    val usedFragmentNames = computeUsedFragmentNames(listOf(midAIr, midBIr, leafIr))

    Truth.assertThat(usedFragmentNames).containsExactly("MidAFragment", "MidBFragment", "RootFragment")

    val (warnings, logger) = newWarningsLogger()
    ApolloCompiler.checkUnusedFragments(
        irOperations = rootIr,
        downstreamUsedFragmentNames = usedFragmentNames.toUsedFragmentNames(),
        options = irOptionsFile.toIrOptions(),
        logger = logger,
    )

    Truth.assertThat(warnings).containsExactly(
        "w: src/test/metadata/fragment-unused-4tier/root.graphql: (5, 1): Apollo: Fragment 'RootUnusedFragment' is not used"
    )
  }

  @Test
  fun `computeUsedFragmentNames merges intermediate-owned fragments from both branches of a diamond`() {
    // "node1" and "node2" each define their own fragment spreading root's fragment; "leaf" depends on both and
    // spreads both intermediate fragments. Root's fragment must be recognized as used via either branch.
    prepareSchemaForChain()

    val rootIr = buildIrOperationsForModule("fragment-unused-diamond", "root", emptyList())
    val node1Ir = buildIrOperationsForModule("fragment-unused-diamond", "node1", rootIr.fragmentDefinitions)
    val node2Ir = buildIrOperationsForModule("fragment-unused-diamond", "node2", rootIr.fragmentDefinitions)
    val leafUpstreamFragmentDefinitions =
      (rootIr.fragmentDefinitions + node1Ir.fragmentDefinitions + node2Ir.fragmentDefinitions).distinctBy { it.name }
    val leafIr = buildIrOperationsForModule("fragment-unused-diamond", "leaf", leafUpstreamFragmentDefinitions)

    val usedFragmentNames = computeUsedFragmentNames(listOf(node1Ir, node2Ir, leafIr))

    Truth.assertThat(usedFragmentNames).containsExactly("Node1Fragment", "Node2Fragment", "RootFragment")

    val (warnings, logger) = newWarningsLogger()
    ApolloCompiler.checkUnusedFragments(
        irOperations = rootIr,
        downstreamUsedFragmentNames = usedFragmentNames.toUsedFragmentNames(),
        options = irOptionsFile.toIrOptions(),
        logger = logger,
    )

    Truth.assertThat(warnings).containsExactly(
        "w: src/test/metadata/fragment-unused-diamond/root.graphql: (5, 1): Apollo: Fragment 'RootUnusedFragment' is not used"
    )
  }

  @Test
  fun `computeUsedFragmentNames does not conflate unrelated sibling fragments that share a name`() {
    // Using siblingA's SharedName must not activate siblingB's unrelated SharedName -> RootUnusedFragment.
    prepareSchemaForChain()

    val rootIr = buildIrOperationsForModule("fragment-unused-sibling-name-clash", "root", emptyList())
    val siblingAIr = buildIrOperationsForModule("fragment-unused-sibling-name-clash", "siblingA", rootIr.fragmentDefinitions)
    val siblingBIr = buildIrOperationsForModule("fragment-unused-sibling-name-clash", "siblingB", rootIr.fragmentDefinitions)

    val usedFragmentNames = computeUsedFragmentNames(listOf(siblingAIr, siblingBIr))

    Truth.assertThat(usedFragmentNames).containsExactly("SharedName")

    val (warnings, logger) = newWarningsLogger()
    ApolloCompiler.checkUnusedFragments(
        irOperations = rootIr,
        downstreamUsedFragmentNames = usedFragmentNames.toUsedFragmentNames(),
        options = irOptionsFile.toIrOptions(),
        logger = logger,
    )

    Truth.assertThat(warnings).containsExactly(
        "w: src/test/metadata/fragment-unused-sibling-name-clash/root.graphql: (1, 1): Apollo: Fragment 'RootUnusedFragment' is not used"
    )
  }

  @Test
  fun `consumer reaches an intermediate fragment despite an unrelated sibling reusing its name`() {
    prepareSchemaForChain()
    val directory = "fragment-unused-sibling-name-clash"
    val rootIr = buildIrOperationsForModule(directory, "root", emptyList())
    val siblingAIr = buildIrOperationsForModule(directory, "siblingA", rootIr.fragmentDefinitions)
    val siblingBIr = buildIrOperationsForModule(directory, "siblingB", rootIr.fragmentDefinitions)
    val childIr = buildIrOperationsForModule(directory, "child", rootIr.fragmentDefinitions + siblingBIr.fragmentDefinitions)

    val usedNames = computeUsedFragmentNames(listOf(siblingAIr, siblingBIr, childIr))
    Truth.assertThat(usedNames).containsExactly("SharedName", "RootUnusedFragment")
    val (warnings, logger) = newWarningsLogger()
    ApolloCompiler.checkUnusedFragments(rootIr, usedNames.toUsedFragmentNames(), irOptionsFile.toIrOptions(), logger)
    Truth.assertThat(warnings).isEmpty()
  }

  @Test
  fun `scoped usage survives serialization and ordering across independent four-tier branches`() {
    for (models in listOf(MODELS_OPERATION_BASED, MODELS_RESPONSE_BASED, MODELS_OPERATION_BASED_WITH_INTERFACES)) {
      prepareSchemaForChain()
      buildIrOptions(codegenModels = models).writeTo(irOptionsFile)
      val directory = "fragment-usage-scoped"
      val rootIr = buildIrOperationsForModule(directory, "root", emptyList())
      val aIr = buildIrOperationsForModule(directory, "a", rootIr.fragmentDefinitions)
      val bIr = buildIrOperationsForModule(directory, "b", rootIr.fragmentDefinitions)
      val aDefinitions = rootIr.fragmentDefinitions + aIr.fragmentDefinitions
      val bDefinitions = rootIr.fragmentDefinitions + bIr.fragmentDefinitions
      val wrapperAIr = buildIrOperationsForModule(directory, "wrapper", aDefinitions)
      val wrapperBIr = buildIrOperationsForModule(directory, "wrapper", bDefinitions)
      val childAIr = buildIrOperationsForModule(directory, "child", aDefinitions + wrapperAIr.fragmentDefinitions)
      val childBIr = buildIrOperationsForModule(directory, "child", bDefinitions + wrapperBIr.fragmentDefinitions)
      val intermediates = listOf(aIr, bIr, wrapperAIr, wrapperBIr)
      val cases = listOf(
          emptyList<IrOperations>() to emptySet<String>(),
          listOf(childAIr) to setOf("Wrapper", "SharedName", "RootA"),
          listOf(childBIr) to setOf("Wrapper", "SharedName", "RootB"),
          listOf(childAIr, childBIr) to setOf("Wrapper", "SharedName", "RootA", "RootB"),
      )
      for ((consumers, expectedNames) in cases) {
        val modules = (intermediates + consumers).mapIndexed { index, ir ->
          val file = File(buildDir, "scoped-$index.json")
          ir.writeTo(file)
          file.toIrOperations()
        }
        for (orderedModules in listOf(modules, modules.reversed())) {
          val usedNames = computeUsedFragmentNames(orderedModules)
          Truth.assertThat(usedNames).containsExactlyElementsIn(expectedNames)
          val (warnings, logger) = newWarningsLogger()
          ApolloCompiler.checkUnusedFragments(rootIr, usedNames.toUsedFragmentNames(), irOptionsFile.toIrOptions(), logger)
          val expectedWarnings = mutableListOf(
              "w: src/test/metadata/fragment-usage-scoped/root.graphql: (9, 1): Apollo: Fragment 'NeverUsed' is not used",
              "w: src/test/metadata/fragment-usage-scoped/root.graphql: (13, 1): Apollo: Fragment 'UnreferencedControl' is not used",
          )
          if (childAIr !in consumers) {
            expectedWarnings.add("w: src/test/metadata/fragment-usage-scoped/root.graphql: (1, 1): Apollo: Fragment 'RootA' is not used")
          }
          if (childBIr !in consumers) {
            expectedWarnings.add("w: src/test/metadata/fragment-usage-scoped/root.graphql: (5, 1): Apollo: Fragment 'RootB' is not used")
          }
          Truth.assertThat(warnings).containsExactlyElementsIn(expectedWarnings)
        }
      }
    }
  }

  @Test
  fun `aggregated usage includes conditional spreads fields and inline fragments`() {
    prepareSchemaForChain()
    val rootIr = buildIrOperationsForModule("fragment-usage-scoped", "root", emptyList())
    val conditionalIr = buildIrOperationsForModule("fragment-usage-scoped", "conditions", rootIr.fragmentDefinitions)
    val serialized = File(buildDir, "conditions.json")
    conditionalIr.writeTo(serialized)
    val usedNames = computeUsedFragmentNames(listOf(serialized.toIrOperations()))

    Truth.assertThat(usedNames).containsExactly(
        "VariableWrapper", "VariableInclude", "RootA", "RootB", "NeverUsed",
        "SkippedSpread", "ExcludedSpread", "SkippedInline", "ExcludedInline", "SkippedField", "ExcludedField",
    )
    val (warnings, logger) = newWarningsLogger()
    ApolloCompiler.checkUnusedFragments(rootIr, usedNames.toUsedFragmentNames(), irOptionsFile.toIrOptions(), logger)
    Truth.assertThat(warnings).containsExactly(
        "w: src/test/metadata/fragment-usage-scoped/root.graphql: (13, 1): Apollo: Fragment 'UnreferencedControl' is not used",
    )
  }

  @Test
  fun `aggregation propagates operation document parse failures`() {
    prepareSchemaForChain()
    val directory = "fragment-usage-scoped"
    val rootIr = buildIrOperationsForModule(directory, "root", emptyList())
    val aIr = buildIrOperationsForModule(directory, "a", rootIr.fragmentDefinitions)
    val wrapperIr = buildIrOperationsForModule(directory, "wrapper", rootIr.fragmentDefinitions + aIr.fragmentDefinitions)
    val childIr = buildIrOperationsForModule(directory, "child", rootIr.fragmentDefinitions + aIr.fragmentDefinitions + wrapperIr.fragmentDefinitions)
    val operation = childIr.operations.single()
    val incompleteIr = childIr.copy(
        operations = listOf(operation.copy(sourceWithFragments = "query GetCharacter {"))
    )

    assertFailsWith<SourceAwareException> {
      computeUsedFragmentNames(listOf(incompleteIr))
    }
  }

  @Test
  fun `undefined fragments fail before usage aggregation`() {
    prepareSchemaForChain()
    val exception = assertFailsWith<SourceAwareException> {
      buildIrOperationsForModule("fragment-undefined", "root", emptyList())
    }
    Truth.assertThat(exception).hasMessageThat().contains("Cannot find fragment `MissingFragment`")
  }

  @Test
  fun `all fragments in an operation-unreachable chain are unused`() {
    prepareSchemaForChain()
    val irOperations = buildIrOperationsForModule("fragment-unused-orphan-chain", "root", emptyList())
    val (warnings, logger) = newWarningsLogger()
    ApolloCompiler.checkUnusedFragments(
        irOperations = irOperations,
        options = irOptionsFile.toIrOptions(),
        logger = logger,
    )
    Truth.assertThat(computeUsedFragmentNames(listOf(irOperations))).isEmpty()
    Truth.assertThat(warnings).containsExactly(
        "w: src/test/metadata/fragment-unused-orphan-chain/root.graphql: (7, 1): Apollo: Fragment 'OrphanA' is not used",
        "w: src/test/metadata/fragment-unused-orphan-chain/root.graphql: (11, 1): Apollo: Fragment 'OrphanB' is not used",
    )
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
