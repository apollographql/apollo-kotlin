package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.ast.GQLFragmentSpread
import com.apollographql.apollo3.ast.GQLInlineFragment
import com.apollographql.apollo3.ast.GQLNode
import com.apollographql.apollo3.ast.introspection.toSchemaGQLDocument
import com.apollographql.apollo3.ast.parseAsGQLDocument
import com.apollographql.apollo3.ast.validateAsSchemaAndAddApolloDefinition
import com.apollographql.apollo3.compiler.TargetLanguage.JAVA
import com.apollographql.apollo3.compiler.TargetLanguage.KOTLIN_1_5
import com.apollographql.apollo3.compiler.TestUtils.checkTestFixture
import com.apollographql.apollo3.compiler.TestUtils.shouldUpdateMeasurements
import com.apollographql.apollo3.compiler.TestUtils.shouldUpdateTestFixtures
import com.apollographql.apollo3.compiler.hooks.ApolloCompilerKotlinHooks
import com.apollographql.apollo3.compiler.hooks.internal.AddInternalCompilerHooks
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import okio.buffer
import okio.source
import org.junit.AfterClass
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@RunWith(TestParameterInjector::class)
@OptIn(ExperimentalTime::class, ApolloExperimental::class)
class CodegenTest {
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

    var outputDir: File
    val codegenDuration = measureTime {
      outputDir = runCodegen(
          folder = folder,
          codegenModels = codegenModels,
          parameters.generateKotlinModels,
      )
    }
    val targetLanguagePath = if (parameters.generateKotlinModels) "kotlin" else "java"

    val expectedRoot = folder.resolve("$targetLanguagePath/$codegenModels")
    // Because codegen will put files under a given packageName, we skip the first 2 folders
    val actualRoot = outputDir.resolve("com/example")

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
      if (parameters.generateKotlinModels) {
        /**
         * Some tests generate warnings because they are using deprecated fields
         *
         * We want to keep this for the user to easily locate them but can't tell the compiler to ignore
         * them specifically. See also https://youtrack.jetbrains.com/issue/KT-24746
         */
        val expectedWarnings = folder.name in listOf(
            "deprecated_merged_field",
            "deprecation",
        )

        KotlinCompiler.assertCompiles(actualFiles.toSet(), !expectedWarnings)
      } else {
        JavaCompiler.assertCompiles(actualFiles.toSet())
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
            val queryFile = file.walk().find { it.extension == "graphql" }
            check(queryFile != null) {
              "Cannot find query file in ${file.absolutePath}"
            }
            val hasFragments = queryFile.source().buffer().parseAsGQLDocument().getOrThrow().hasFragments()

            when {
              file.name == "companion" -> listOf(Parameters(file, MODELS_OPERATION_BASED, true))
              hasFragments -> {
                @Suppress("DEPRECATION")
                val list = listOf(
                    Parameters(file, MODELS_OPERATION_BASED, true),
                )

                if (file.name in listOf("inline_fragment_with_include_directive", "fragment_spread_with_include_directive", "fragments_with_defer_and_include_directives")) {
                  // These do not support responseBased models because of include or defer directives on fragments
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
              // add Java if supported
              if (it.first().folder.name != "big_query") {
                add(it.first().copy(generateKotlinModels = false, codegenModels = MODELS_OPERATION_BASED))
              }
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
          "%-80s %20s\n",
          "aggregate-$name",
          filtered.map { it.linesOfCode }.fold(0L) { acc, i -> acc + i }.toString(),
      )
    }

    @AfterClass
    @JvmStatic
    fun dumpTimes() {
      if (shouldUpdateMeasurements()) {
        File("src/test/graphql/com/example/measurements").apply {
          writeText("""
            // This file keeps track of the size of generated code to avoid blowing up the codegen size.
            // If you updated the codegen and test fixtures, you should commit this file too.
          """.trimIndent())

          appendText("\n\n")

          appendText(
              String.format(
                  "%-80s %20s\n",
                  "Test:",
                  "Total LOC:",
              )
          )
          appendText(aggregate("all") { true })
          appendText(aggregate("kotlin-responseBased") { Regex(".*kotlin-responseBased-.*").matches(it.name) })
          appendText(aggregate("kotlin-operationBased") { Regex(".*kotlin-operationBased-.*").matches(it.name) })
          appendText(aggregate("kotlin-compat") { Regex(".*kotlin-compat-.*").matches(it.name) })
          appendText(aggregate("java-operationBased") { Regex(".*java-operationBased-.*").matches(it.name) })
          appendText("\n")
          appendText(
              measurements.sortedByDescending { it.linesOfCode }
                  .joinToString("\n") { measurement ->
                    String.format(
                        "%-80s %20s",
                        measurement.name,
                        measurement.linesOfCode.toString(),
                    )
                  }
          )
        }
      }
    }

    private fun runCodegen(folder: File, codegenModels: String, generateKotlinModels: Boolean): File {
      val useSemanticNaming = when (folder.name) {
        "hero_details_semantic_naming" -> true
        "mutation_create_review_semantic_naming" -> true
        else -> false
      }
      val generateAsInternal = when (folder.name) {
        "mutation_create_review", "simple_fragment" -> true
        else -> false
      }
      val generateDataBuilders = when (folder.name) {
        "mutation_create_review", "simple_fragment", "data_builders" -> true
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


      val sealedClassesForEnumsMatching = when (folder.name) {
        "enums_as_sealed" -> listOf(".*")
        else -> emptyList()
      }

      val classesForEnumsMatching = when (folder.name) {
        "enum_field" -> listOf("Gravity")
        else -> listOf(".*")
      }

      val generateModelBuilders = when (folder.name) {
        "fragment_with_inline_fragment", "java_primitive_types", "java_apollo_optionals", "java_guava_optionals", "java_java_optionals",
        "simple_target_name", "java_jetbrains_annotations", "java_android_annotations", "java_jsr305_annotations",
        -> true

        else -> false
      }

      val generateSchema = folder.name == "__schema"

      val schemaFile = folder.listFiles()!!.find { it.isFile && (it.name == "schema.sdl" || it.name == "schema.json" || it.name == "schema.graphqls") }
          ?: File("src/test/graphql/schema.sdl")

      val graphqlFiles = setOf(File(folder, "TestOperation.graphql"))
      val operationOutputGenerator = OperationOutputGenerator.Default(operationIdGenerator)

      val targetLanguage = if (generateKotlinModels) KOTLIN_1_5 else JAVA
      val targetLanguagePath = if (generateKotlinModels) "kotlin" else "java"
      val flattenModels = when {
        folder.name in listOf("capitalized_fields", "companion") -> true
        targetLanguage == JAVA -> true
        else -> false
      }
      val scalarMapping = if (folder.name in listOf(
              "custom_scalar_type",
              "input_object_type",
              "mutation_create_review")) {
        if (targetLanguage == JAVA) {
          mapOf(
              "Date" to ScalarInfo("java.util.Date"),
              "URL" to ScalarInfo("java.lang.String", ExpressionAdapterInitializer("com.example.UrlAdapter.INSTANCE")),
              "ID" to ScalarInfo("java.lang.Long"),
              "String" to ScalarInfo("java.lang.String", ExpressionAdapterInitializer("new com.example.MyStringAdapter()")),
          )
        } else {
          mapOf(
              "Date" to ScalarInfo("java.util.Date"),
              "URL" to ScalarInfo("kotlin.String", ExpressionAdapterInitializer("com.example.UrlAdapter")),
              "ID" to ScalarInfo("kotlin.Long"),
              "String" to ScalarInfo("kotlin.String", ExpressionAdapterInitializer("com.example.MyStringAdapter()")),
          )

        }
      } else {
        emptyMap()
      }

      val addJvmOverloads = when (folder.name) {
        "variable_default_value" -> true
        else -> defaultAddJvmOverloads
      }

      val packageName = "com.example.${folder.name}"
      val outputDir = File("build/generated/test/${folder.name}/$targetLanguagePath/$codegenModels/")

      val generatePrimitiveTypes = when (folder.name) {
        "java_primitive_types", "java_apollo_optionals", "java_guava_optionals", "java_java_optionals", "java_jetbrains_annotations",
        "java_android_annotations", "java_jsr305_annotations",
        -> true

        else -> false
      }

      val nullableFieldStyle = when (folder.name) {
        "java8annotation" -> JavaNullable.JETBRAINS_ANNOTATIONS
        "java_apollo_optionals" -> JavaNullable.APOLLO_OPTIONAL
        "java_guava_optionals" -> JavaNullable.GUAVA_OPTIONAL
        "java_java_optionals" -> JavaNullable.JAVA_OPTIONAL
        "java_jetbrains_annotations" -> JavaNullable.JETBRAINS_ANNOTATIONS
        "java_android_annotations" -> JavaNullable.ANDROID_ANNOTATIONS
        "java_jsr305_annotations" -> JavaNullable.JSR_305_ANNOTATIONS
        else -> JavaNullable.NONE
      }

      val decapitalizeFields = when (folder.name) {
        "decapitalized_fields" -> true
        else -> false
      }

      val requiresOptInAnnotation = when (folder.name) {
        "suppressed_warnings" -> "com.apollographql.apollo3.annotations.ApolloRequiresOptIn"
        else -> "none"
      }

      val compilerKotlinHooks = if (generateAsInternal) AddInternalCompilerHooks(setOf(".*")) else ApolloCompilerKotlinHooks.Identity
      val packageNameGenerator = PackageNameGenerator.Flat(packageName)

      ApolloCompiler.writeSimple(
          schema = schemaFile.toSchemaGQLDocument().validateAsSchemaAndAddApolloDefinition().getOrThrow(),
          executableFiles = graphqlFiles,
          outputDir = outputDir,
          flattenModels = flattenModels,
          codegenModels = codegenModels,
          decapitalizeFields = decapitalizeFields,
          operationOutputGenerator = operationOutputGenerator,
          useSemanticNaming = useSemanticNaming,
          packageNameGenerator = packageNameGenerator,
          generateFragmentImplementations = generateFragmentImplementations,
          generateSchema = generateSchema,
          schemaPackageName = packageName,
          scalarMapping = scalarMapping,
          generateDataBuilders = generateDataBuilders,
          targetLanguage = targetLanguage,
          nullableFieldStyle = nullableFieldStyle,
          generateModelBuilders = generateModelBuilders,
          classesForEnumsMatching = classesForEnumsMatching,
          sealedClassesForEnumsMatching = sealedClassesForEnumsMatching,
          addJvmOverloads = addJvmOverloads,
          requiresOptInAnnotation = requiresOptInAnnotation,
          compilerKotlinHooks = compilerKotlinHooks,
          generatePrimitiveTypes = generatePrimitiveTypes,
          generateFilterNotNull = true
      )
      return outputDir
    }

    private fun GQLNode.hasFragments(): Boolean {
      if (this is GQLInlineFragment || this is GQLFragmentSpread) {
        return true
      }
      return children.any { it.hasFragments() }
    }
  }
}
