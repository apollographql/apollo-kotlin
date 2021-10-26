package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.ast.GQLFragmentSpread
import com.apollographql.apollo3.ast.GQLInlineFragment
import com.apollographql.apollo3.ast.GQLNode
import com.apollographql.apollo3.ast.parseAsGQLDocument
import com.apollographql.apollo3.compiler.TestUtils.checkTestFixture
import com.apollographql.apollo3.compiler.TestUtils.shouldUpdateMeasurements
import com.apollographql.apollo3.compiler.TestUtils.shouldUpdateTestFixtures
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import org.junit.AfterClass
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@RunWith(TestParameterInjector::class)
@OptIn(ExperimentalTime::class)
class CodegenTest() {
  private class Measurement(
      val name: String,
      val linesOfCode: Int,
      val codegenDuration: Duration,
      val compileDuration: Duration,
  )

  data class Parameters(val folder: File, val codegenModels: String, val generateKotlinModels: Boolean) {
    override fun toString(): String {
      val targetLanguage = if (generateKotlinModels) "kotlin" else "java"
      return "$targetLanguage-$codegenModels-${folder.name}"
    }
  }

  @Test
  fun generateExpectedClasses(@TestParameter(valuesProvider = ParametersProvider::class) parameters: Parameters) {
    val folder = parameters.folder
    val codegenModels = parameters.codegenModels

    val options = options(
        folder = folder,
        codegenModels = codegenModels,
        parameters.generateKotlinModels,
    )
    options.outputDir.deleteRecursively()

    val codegenDuration = measureTime {
      GraphQLCompiler.write(options)
    }

    val expectedRoot = folder.resolve("${options.targetLanguage}/$codegenModels")
    // Because codegen will put files under a given packageName, we skip the first 2 folders
    val actualRoot = options.outputDir.resolve("com/example")

    val actualFiles = actualRoot.walk().filter {
      it.isFile
    }

    val expectedFiles = expectedRoot.walk().filter {
      it.isFile && it.extension == "expected"
    }

    expectedFiles.forEach { expected ->
      val relativePath = expected.relativeTo(expectedRoot).path.removeSuffix(".expected")
      val actual = actualRoot.resolve(relativePath)
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
      val relativePath = actual.relativeTo(actualRoot).path
      val expected = expectedRoot.resolve("$relativePath.expected")
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
    val compileDuration = measureTime {
      when (options.targetLanguage) {
        TARGET_KOTLIN -> {
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
              "__schema"
          )

          KotlinCompiler.assertCompiles(actualFiles.toSet(), !expectedWarnings)
        }
        TARGET_JAVA -> JavaCompiler.assertCompiles(actualFiles.toSet())
        else -> error("No compiler found for ${options.targetLanguage}")
      }
    }

    measurements.add(
        Measurement(
            name = parameters.toString(),
            linesOfCode = totalLineOfCode,
            codegenDuration = codegenDuration,
            compileDuration = compileDuration,
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

  class ParametersProvider : TestParameter.TestParameterValuesProvider {
    @OptIn(ExperimentalStdlibApi::class)
    override fun provideValues(): List<Parameters> {
      return File("src/test/graphql/com/example/")
          .listFiles()!!
          .filter { it.isDirectory }
          .sortedBy { it.name }
          .map { file ->
            val queryFile = checkNotNull(file.walk().find { it.extension == "graphql" })
            val hasFragments = queryFile.parseAsGQLDocument().getOrThrow().hasFragments()

            when {
              hasFragments -> {
                val list = listOf(
                    Parameters(file, MODELS_OPERATION_BASED, true),
                    Parameters(file, MODELS_COMPAT, true)
                )

                if (file.name in listOf("inline_fragment_with_include_directive", "fragment_spread_with_include_directive")) {
                  // These do not support responseBased models because of include directives on fragments
                  list
                } else {
                  list + listOf(Parameters(file, MODELS_RESPONSE_BASED, true))
                }
              }
              else -> {
                listOf(
                    Parameters(file, MODELS_RESPONSE_BASED, true)
                )
              }
            }
          }.map {
            buildList {
              addAll(it)
              // add Java
              add(it.first().copy(generateKotlinModels = false, codegenModels = MODELS_OPERATION_BASED))
            }
          }
          .flatten()
          .filter { params ->
            TestUtils.testFilterMatches(params.toString())
          }

    }
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
                  "Total LOC:",
                  "Codegen (ms):",
                  "Compilation (ms):",
              )
          )
          appendText(aggregate("all") { true })
          appendText(aggregate("responseBased") { Regex(".*-responseBased-.*").matches(it.name) })
          appendText(aggregate("operationBased") { Regex(".*-operationBased-.*").matches(it.name) })
          appendText(aggregate("compat") { Regex(".*-compat-.*").matches(it.name) })
          appendText("\n")
          appendText(
              measurements.sortedByDescending { it.linesOfCode }
                  .joinToString("\n") { measurement ->
                    String.format(
                        "%-50s %20s %20s %20s",
                        measurement.name,
                        measurement.linesOfCode.toString(),
                        measurement.codegenDuration.toLong(TimeUnit.MILLISECONDS).toString(),
                        measurement.compileDuration.toString(),
                    )
                  }
          )
        }
      }
    }

    private fun options(folder: File, codegenModels: String, generateKotlinModels: Boolean): Options {
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

      val generateSchema = folder.name == "__schema"

      val schemaFile = folder.listFiles()!!.find { it.isFile && (it.name == "schema.sdl" || it.name == "schema.json" || it.name == "schema.graphqls") }
          ?: File("src/test/graphql/schema.sdl")

      val graphqlFiles = setOf(File(folder, "TestOperation.graphql"))
      val operationOutputGenerator = OperationOutputGenerator.Default(operationIdGenerator)

      val targetLanguage = if (generateKotlinModels) TARGET_KOTLIN else TARGET_JAVA
      val flattenModels = when {
        targetLanguage == TARGET_JAVA -> true
        else -> codegenModels == MODELS_COMPAT
      }

      return Options(
          executableFiles = graphqlFiles,
          schemaFile = schemaFile,
          outputDir = File("build/generated/test/${folder.name}/$targetLanguage/$codegenModels/"),
          packageName = "com.example.${folder.name}"
      ).copy(
          operationOutputGenerator = operationOutputGenerator,
          customScalarsMapping = customScalarsMapping,
          codegenModels = codegenModels,
          flattenModels = flattenModels,
          useSemanticNaming = useSemanticNaming,
          generateAsInternal = generateAsInternal,
          generateFilterNotNull = true,
          generateFragmentImplementations = generateFragmentImplementations,
          generateSchema = generateSchema,
          moduleName = folder.name,
          targetLanguage = targetLanguage,
          targetLanguageVersion = "1.5.0",
      )
    }

    private fun GQLNode.hasFragments(): Boolean {
      if (this is GQLInlineFragment || this is GQLFragmentSpread) {
        return true
      }
      return children.any { it.hasFragments() }
    }
  }
}
