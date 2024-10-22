package com.apollographql.apollo.compiler

import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.ast.GQLFragmentSpread
import com.apollographql.apollo.ast.GQLInlineFragment
import com.apollographql.apollo.ast.GQLNode
import com.apollographql.apollo.ast.parseAsGQLDocument
import com.apollographql.apollo.compiler.ApolloCompiler.Logger
import com.apollographql.apollo.compiler.TargetLanguage.JAVA
import com.apollographql.apollo.compiler.TargetLanguage.KOTLIN_1_5
import com.apollographql.apollo.compiler.TargetLanguage.KOTLIN_1_9
import com.apollographql.apollo.compiler.TestUtils.checkTestFixture
import com.apollographql.apollo.compiler.TestUtils.shouldUpdateMeasurements
import com.apollographql.apollo.compiler.TestUtils.shouldUpdateTestFixtures
import com.apollographql.apollo.compiler.codegen.SourceOutput
import com.apollographql.apollo.compiler.codegen.java.JavaOutput
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinOutput
import com.apollographql.apollo.compiler.codegen.writeTo
import com.apollographql.apollo.compiler.ir.IrOperations
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import org.junit.AfterClass
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.time.Duration
import kotlin.time.measureTime

@RunWith(TestParameterInjector::class)
@OptIn(ApolloExperimental::class)
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
        KotlinCompiler.assertCompiles(actualFiles.toSet())
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
            val hasFragments = queryFile.parseAsGQLDocument().getOrThrow().hasFragments()

            when {
              file.name == "companion" -> listOf(Parameters(file, MODELS_OPERATION_BASED, true))
              hasFragments -> {
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
          """.trimIndent()
          )

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

      val generateDataBuilders = when (folder.name) {
        "mutation_create_review", "simple_fragment", "data_builders" -> true
        else -> false
      }
      @Suppress("DEPRECATION") val operationIdGenerator = when (folder.name) {
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

      val schemaFile =
        folder.listFiles()!!.find { it.isFile && (it.name == "schema.sdl" || it.name == "schema.json" || it.name == "schema.graphqls") }
            ?: File("src/test/graphql/schema.sdl")

      val graphqlFiles = setOf(File(folder, "TestOperation.graphql"))

      @Suppress("DEPRECATION")
      val operationOutputGenerator = OperationOutputGenerator.Default(operationIdGenerator)

      @Suppress("DEPRECATION")
      val targetLanguage = if (generateKotlinModels) {
        if (folder.name == "enum_field") KOTLIN_1_9 else KOTLIN_1_5
      } else {
        JAVA
      }
      val targetLanguagePath = if (generateKotlinModels) "kotlin" else "java"
      val flattenModels = when {
        folder.name in listOf("capitalized_fields", "companion") -> true
        targetLanguage == JAVA -> true
        else -> false
      }
      val scalarMapping = if (folder.name in listOf(
              "custom_scalar_type",
              "input_object_type",
              "mutation_create_review"
          )
      ) {
        if (targetLanguage == JAVA) {
          mapOf(
              "Date" to ScalarInfo("java.util.Date"),
              "URL" to ScalarInfo("java.lang.String", ExpressionAdapterInitializer("com.example.UrlAdapter.INSTANCE")),
              "ID" to ScalarInfo("java.lang.Long"),
              "String" to ScalarInfo("java.lang.String", ExpressionAdapterInitializer("new com.example.MyStringAdapter()")),
              "ListOfString" to ScalarInfo("List<String>"),
          )
        } else {
          mapOf(
              "Date" to ScalarInfo("java.util.Date"),
              "URL" to ScalarInfo("kotlin.String", ExpressionAdapterInitializer("com.example.UrlAdapter")),
              "ID" to ScalarInfo("kotlin.Long"),
              "String" to ScalarInfo("kotlin.String", ExpressionAdapterInitializer("com.example.MyStringAdapter()")),
              "ListOfString" to ScalarInfo("List<String?>"),
          )
        }
      } else {
        emptyMap()
      }

      val packageName = "com.example.${folder.name}"
      val outputDir = File("build/generated/test/${folder.name}/$targetLanguagePath/$codegenModels/")

      val decapitalizeFields = when (folder.name) {
        "decapitalized_fields" -> true
        else -> false
      }

      val generateMethods = when (targetLanguage) {
        JAVA -> null
        else -> {
          if (folder.name == "input_object_variable_and_argument_with_generated_methods") {
            listOf(GeneratedMethod.COPY, GeneratedMethod.TO_STRING, GeneratedMethod.EQUALS_HASH_CODE)
          } else {
            null
          }
        }
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

      val generatePrimitiveTypes = when (folder.name) {
        "java_primitive_types", "java_apollo_optionals", "java_guava_optionals", "java_java_optionals", "java_jetbrains_annotations",
        "java_android_annotations", "java_jsr305_annotations",
          -> true

        else -> false
      }

      val sealedClassesForEnumsMatching = when (folder.name) {
        "enums_as_sealed" -> listOf(".*")
        "enum_field" -> listOf("Gravity")
        else -> emptyList()
      }

      val generateInputBuilders = folder.name == "input_object_type"


      val addJvmOverloads = when (folder.name) {
        "variable_default_value" -> true
        else -> defaultAddJvmOverloads
      }

      val requiresOptInAnnotation = when (folder.name) {
        "suppressed_warnings" -> "com.apollographql.apollo.annotations.ApolloRequiresOptIn"
        else -> "none"
      }

      val generateAsInternal = when (folder.name) {
        "mutation_create_review", "simple_fragment", "enum_field" -> true
        else -> false
      }

      val codegenOptions = buildCodegenOptions(
          targetLanguage = targetLanguage,
          useSemanticNaming = useSemanticNaming,
          packageName = packageName,
          generateFragmentImplementations = generateFragmentImplementations,
          generateSchema = generateSchema,
          generateMethods = generateMethods,
          nullableFieldStyle = nullableFieldStyle.takeIf { targetLanguage == JAVA },
          classesForEnumsMatching = classesForEnumsMatching.takeIf { targetLanguage == JAVA },
          generateModelBuilders = generateModelBuilders.takeIf { targetLanguage == JAVA },
          generatePrimitiveTypes = generatePrimitiveTypes.takeIf { targetLanguage == JAVA },
          sealedClassesForEnumsMatching = sealedClassesForEnumsMatching.takeIf { targetLanguage != JAVA },
          generateInputBuilders = generateInputBuilders.takeIf { targetLanguage != JAVA },
          addJvmOverloads = addJvmOverloads.takeIf { targetLanguage != JAVA },
          requiresOptInAnnotation = requiresOptInAnnotation.takeIf { targetLanguage != JAVA },
          generateAsInternal = generateAsInternal.takeIf { targetLanguage != JAVA },
          generateFilterNotNull = true.takeIf { targetLanguage != JAVA },
          decapitalizeFields = decapitalizeFields
      )

      val (irOperations, sourceOutput) = ApolloCompiler.buildSchemaAndOperationsSourcesAndReturnIrOperations(
          schemaFiles = setOf(schemaFile).toInputFiles(),
          executableFiles = graphqlFiles.toInputFiles(),
          codegenSchemaOptions = buildCodegenSchemaOptions(
              scalarMapping = scalarMapping,
              generateDataBuilders = generateDataBuilders
          ),
          irOptions = buildIrOptions(
              codegenModels = codegenModels,
              flattenModels = flattenModels,
              decapitalizeFields = decapitalizeFields,
          ),
          codegenOptions = codegenOptions,
          operationOutputGenerator = operationOutputGenerator,
          logger = null,
          layoutFactory = null,
          operationManifestFile = null,
          irOperationsTransform = null,
          javaOutputTransform = null,
          kotlinOutputTransform = null,
      )

      sourceOutput.writeTo(outputDir, true, null)
      irOperations.usedCoordinates.writeTo(File(outputDir, "com/example/used-coordinates.json"))
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

/**
 * Same as ApolloCompiler.buildSchemaAndOperationsSources but also returns the IrOperations so we can inspect their contents.
 */
private fun ApolloCompiler.buildSchemaAndOperationsSourcesAndReturnIrOperations(
    schemaFiles: List<InputFile>,
    executableFiles: List<InputFile>,
    codegenSchemaOptions: CodegenSchemaOptions,
    irOptions: IrOptions,
    codegenOptions: CodegenOptions,
    layoutFactory: LayoutFactory?,
    @Suppress("DEPRECATION") operationOutputGenerator: OperationOutputGenerator?,
    irOperationsTransform: Transform<IrOperations>?,
    javaOutputTransform: Transform<JavaOutput>?,
    kotlinOutputTransform: Transform<KotlinOutput>?,
    logger: Logger?,
    operationManifestFile: File?,
): Pair<IrOperations, SourceOutput> {
  val codegenSchema = buildCodegenSchema(
      schemaFiles = schemaFiles,
      logger = logger,
      codegenSchemaOptions = codegenSchemaOptions,
      foreignSchemas = emptyList()
  )

  val irOperations = buildIrOperations(
      codegenSchema = codegenSchema,
      executableFiles = executableFiles,
      upstreamCodegenModels = emptyList(),
      upstreamFragmentDefinitions = emptyList(),
      documentTransform = null,
      options = irOptions,
      logger = logger
  )

  val sourceOutput = buildSchemaAndOperationsSourcesFromIr(
      codegenSchema = codegenSchema,
      irOperations = irOperations,
      downstreamUsedCoordinates = UsedCoordinates(),
      upstreamCodegenMetadata = emptyList(),
      codegenOptions = codegenOptions,
      layout = layoutFactory?.create(codegenSchema),
      irOperationsTransform = irOperationsTransform,
      javaOutputTransform = javaOutputTransform,
      kotlinOutputTransform = kotlinOutputTransform,
      operationManifestFile = operationManifestFile,
      operationOutputGenerator = operationOutputGenerator,
  )

  return irOperations to sourceOutput
}
