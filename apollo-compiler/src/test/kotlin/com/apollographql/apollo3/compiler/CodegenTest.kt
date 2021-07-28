package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.ast.GQLFragmentSpread
import com.apollographql.apollo3.ast.GQLInlineFragment
import com.apollographql.apollo3.ast.GQLNode
import com.apollographql.apollo3.ast.parseAsGQLDocument
import com.apollographql.apollo3.compiler.TestUtils.checkTestFixture
import com.apollographql.apollo3.compiler.TestUtils.shouldUpdateMeasurements
import com.apollographql.apollo3.compiler.TestUtils.shouldUpdateTestFixtures
import org.junit.AfterClass
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@RunWith(Parameterized::class)
@OptIn(ExperimentalTime::class)
class CodegenTest(private val folder: File, private val codegenModels: String, private val hasFragments: Boolean) {
  private class Measurement(
      val name: String,
      val codegenModels: String,
      val hasFragments: Boolean,
      val linesOfCode: Int,
      val codegenDuration: Duration,
      val compileDuration: Duration,
  )


  @Test
  fun generateExpectedClasses() {
    val args = arguments(
        folder = folder,
        codegenModels = codegenModels,
    )
    generateExpectedClasses(args, hasFragments)
  }

  private fun generateExpectedClasses(options: Options, hasFragments: Boolean) {
    options.outputDir.deleteRecursively()

    val codegenDuration = measureTime {
      GraphQLCompiler.write(options)
    }

    val expectedRoot = folder.parentFile.parentFile.parentFile
    val expectedRelativeRoot = folder.relativeTo(expectedRoot)

    val actualRoot = options.outputDir
    val actualFiles = actualRoot.walk().filter {
      // extension should always be the correct one, it's a bug else
      it.isFile && it.name != "metadata"
    }

    val expectedFiles = folder.resolve(codegenModels).walk().filter { it.isFile && it.extension == "expected" }

    expectedFiles.forEach { expected ->
      val relativePath = expected.relativeTo(folder.resolve(codegenModels)).path.removeSuffix(".expected")

      val actual = actualRoot.resolve(expectedRelativeRoot).resolve(relativePath)
      if (!actual.exists()) {
        if (shouldUpdateTestFixtures()) {
          println("removing actual file: ${expected.absolutePath}")
          expected.delete()
          return@forEach
        } else {
          throw Exception("No actual file for ${actual.absolutePath}")
        }
      }

      // Do not generate a diff everytime the version changes
      actual.replaceVersionWithPlaceHolder()
      checkTestFixture(actual = actual, expected = expected)
    }

    actualFiles.forEach { actual ->
      val relativePath = actual.relativeTo(actualRoot).relativeTo(expectedRelativeRoot).path
      val expected = expectedRoot.resolve(expectedRelativeRoot).resolve(codegenModels).resolve("$relativePath.expected")
      if (!expected.exists()) {
        if (shouldUpdateTestFixtures()) {
          println("adding expected file: ${actual.absolutePath} - ${actual.path}")
          expected.parentFile.mkdirs()
          actual.replaceVersionWithPlaceHolder()
          actual.copyTo(expected)
          return@forEach
        } else {
          throw Exception("No expected file at ${expected.absolutePath}")
        }
      }
      // no need to call checkTestFixture again, this has been taken care of
    }

    val totalLineOfCode = if (shouldUpdateMeasurements()) {
      expectedFiles.fold(0) { totalCount, file -> totalCount + file.readLines().size }
    } else -1

    /**
     * Check that generated sources compile
     */

    /**
     * Some tests generate warnings.
     * Most of the time because they are using deprecated fields.
     * Fine tune this list as we go.
     */
    val expectedWarnings = folder.name in listOf(
        "arguments_complex",
        "arguments_simple",
        "case_sensitive_enum",
        "custom_scalar_type",
        "deprecated_merged_field",
        "deprecation",
        "enum_field",
        "fragment_with_inline_fragment",
        "hero_name_query_long_name",
        "hero_with_review",
        "inline_fragments_with_friends",
        "input_object_type",
        "input_object_variable_and_argument",
        "mutation_create_review",
        "mutation_create_review_semantic_naming",
        "named_fragment_inside_inline_fragment",
        "nested_conditional_inline",
        "optional",
        "root_query_inline_fragment",
        "union_inline_fragments",
        "unique_type_name",
        "variable_default_value",
    )
    val compileDuration = measureTime {
      KotlinCompiler.assertCompiles(actualFiles.toSet(), !expectedWarnings)
    }

    measurements.add(
        Measurement(
            name = options.moduleName,
            codegenModels = codegenModels,
            linesOfCode = totalLineOfCode,
            codegenDuration = codegenDuration,
            compileDuration = compileDuration,
            hasFragments = hasFragments,
        )
    )
  }

  private fun File.replaceVersionWithPlaceHolder() {
    writeText(
        readText().replace(
            "This class was automatically generated by Apollo GraphQL version '$APOLLO_VERSION'",
            "This class was automatically generated by Apollo GraphQL version '${'$'}VERSION'"
        )
    )
  }

  companion object {
    private val measurements = mutableListOf<Measurement>()

    private fun aggregate(name: String, filter: (Measurement) -> Boolean): String {
      val filtered = measurements.filter { filter(it) }
      return String.format(
          "%-50s %-20s %20s %20s %20s\n",
          "aggregate",
          name,
          filtered.map { it.linesOfCode }.fold(0L) { acc, i -> acc + i }.toString(),
          filtered.map { it.codegenDuration }.fold(Duration.ZERO) { acc, measurement -> acc + measurement }.toString(),
          filtered.map { it.compileDuration }.fold(Duration.ZERO) { acc, measurement -> acc + measurement }.toString(),
      )
    }
    @AfterClass
    @JvmStatic
    fun dumpTimes() {
      if (shouldUpdateMeasurements()) {
        File("src/test/graphql/com/example/measurements").apply {
          writeText(
              String.format(
                  "%-50s %-20s %20s %20s\n",
                  "Test:",
                  "CodegenModels:",
                  "Total LOC:",
                  "Codegen (ms):",
                  "Compilation (ms):",
              )
          )
          appendText(aggregate("all") { true })
          appendText(aggregate("responseBased") { it.codegenModels == "responseBased" && it.hasFragments})
          appendText(aggregate("operationBased") { it.codegenModels == "operationBased"&& it.hasFragments })
          appendText(aggregate("compat") { it.codegenModels == "compat" && it.hasFragments})
          appendText(aggregate("no-fragments") { !it.hasFragments})
          appendText("\n")
          appendText(
              measurements.sortedByDescending { it.linesOfCode }
                  .joinToString("\n") { measurement ->
                    String.format(
                        "%-50s %-20s %20s %20s %20s",
                        measurement.name,
                        measurement.codegenModels,
                        measurement.linesOfCode.toString(),
                        measurement.codegenDuration.toLong(TimeUnit.MILLISECONDS).toString(),
                        measurement.compileDuration.toString(),
                    )
                  }
          )
        }
      }
    }

    private fun arguments(folder: File, codegenModels: String): Options {
      val customScalarsMapping = if (folder.name in listOf(
              "custom_scalar_type",
              "input_object_type",
              "mutation_create_review")) {
        mapOf(
            "Date" to "java.util.Date",
            "URL" to "java.lang.String",
        )
      } else {
        emptyMap()
      }
      val useSemanticNaming = when (folder.name) {
        "hero_details_semantic_naming" -> true
        "mutation_create_review_semantic_naming" -> true
        else -> false
      }
      val generateAsInternal = when (folder.name) {
        "mutation_create_review", "simple_fragment" -> true
        else -> false
      }
      val operationIdGenerator = when (folder.name) {
        "operation_id_generator" -> object : OperationIdGenerator {
          override fun apply(operationDocument: String, operationName: String): String {
            return "hash"
          }

          override val version: String = "1"
        }
        else -> OperationIdGenerator.Sha256
      }

      val generateFragmentImplementations = when (folder.name) {
        "named_fragment_without_implementation" -> false
        else -> true
      }

      val schemaFile = folder.listFiles()!!.find { it.isFile && (it.name == "schema.sdl" || it.name == "schema.json" || it.name == "schema.graphqls") }
          ?: File("src/test/graphql/schema.sdl")

      val graphqlFiles = setOf(File(folder, "TestOperation.graphql"))
      val operationOutputGenerator = OperationOutputGenerator.Default(operationIdGenerator)

      return Options(
          executableFiles = graphqlFiles,
          outputDir = File("build/generated/test/${folder.name}"),
          operationOutputGenerator = operationOutputGenerator,
          useSemanticNaming = useSemanticNaming,
          generateAsInternal = generateAsInternal,
          generateFilterNotNull = true,
          generateFragmentImplementations = generateFragmentImplementations,
          moduleName = folder.name,
          schemaFile = schemaFile,
          customScalarsMapping = customScalarsMapping,
          codegenModels = codegenModels,
          packageName = "com.example.${folder.name}",
          flattenModels = codegenModels == MODELS_COMPAT
      )
    }

    @JvmStatic
    @Parameterized.Parameters(name = "{0} ({1})")
    fun data(): Collection<*> {
      return File("src/test/graphql/com/example/")
          .listFiles()!!
          .filter { it.isDirectory }
          .sortedBy { it.name }
          .filter { file ->
            TestUtils.testFilterMatches(file.absolutePath)
          }
          .flatMap { file ->
            val codegenModels = System.getProperty("codegenModels")?.trim()
            val queryFile = checkNotNull(file.walk().find { it.extension == "graphql" })
            val hasFragments = queryFile.parseAsGQLDocument().getOrThrow().hasFragments()

            when {
              !codegenModels.isNullOrBlank() -> {
                listOf(
                    arrayOf(file, codegenModels, true),
                )
              }
              hasFragments -> {
                val list = listOf(
                    arrayOf(file, MODELS_OPERATION_BASED, true),
                    arrayOf(file, MODELS_COMPAT, true)
                )

                if (file.name in listOf("inline_fragment_with_include_directive", "fragment_spread_with_include_directive")) {
                  // These do not suport responseBased models
                  list
                } else {
                  list + listOf(arrayOf(file, MODELS_RESPONSE_BASED, true))
                }
              }
              else -> {
                listOf(
                    arrayOf(file, MODELS_RESPONSE_BASED, false)
                )
              }
            }
          }
    }

    private fun GQLNode.hasFragments(): Boolean {
      if (this is GQLInlineFragment || this is GQLFragmentSpread) {
        return true
      }
      return children.any { it.hasFragments() }
    }
  }
}
