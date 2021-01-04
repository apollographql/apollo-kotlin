package com.apollographql.apollo.compiler

import com.apollographql.apollo.compiler.TestUtils.checkTestFixture
import com.apollographql.apollo.compiler.TestUtils.shouldUpdateTestFixtures
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class CodegenTest(private val folder: File) {

  @Test
  fun generateExpectedClasses() {
    val args = arguments(folder = folder)
    generateExpectedClasses(args)
  }

  private fun generateExpectedClasses(args: GraphQLCompiler.Arguments) {
    args.outputDir.deleteRecursively()
    GraphQLCompiler().write(args)

    val expectedRoot = folder.parentFile.parentFile.parentFile
    val expectedFiles = folder.walk().filter { it.isFile && it.extension == "kt" }

    val actualRoot = args.outputDir
    val actualFiles = actualRoot.walk().filter {
      // extension should always be the correct one, it's a bug else
      it.isFile && it.name != "metadata"
    }

    expectedFiles.forEach { expected ->
      val relativePath = expected.relativeTo(expectedRoot).path
      val actual = File(actualRoot, relativePath)
      if (!actual.exists()) {
        if (shouldUpdateTestFixtures()) {
          println("removing actual file: ${expected.absolutePath}")
          expected.delete()
          return@forEach
        } else {
          throw Exception("No actual file for ${actual.absolutePath}")
        }
      }
      checkTestFixture(actual = actual, expected = expected)
    }
    actualFiles.forEach { actual ->
      val relativePath = actual.relativeTo(actualRoot).path
      val expected = File(expectedRoot, relativePath)
      if (!expected.exists()) {
        if (shouldUpdateTestFixtures()) {
          println("adding expected file: ${actual.absolutePath} - ${actual.path}")
          actual.copyTo(expected)
          return@forEach
        } else {
          throw Exception("No expected file for ${expected.absolutePath}")
        }
      }
      // no need to call checkTestFixture again, this has been taken care of
    }

    // And that they compile
    val expectedWarnings = folder.name in listOf("deprecation", "custom_scalar_type_warnings", "arguments_complex", "arguments_simple")
    KotlinCompiler.assertCompiles(actualFiles.toSet(), !expectedWarnings)
  }

  companion object {
    fun arguments(folder: File): GraphQLCompiler.Arguments {
      val customScalarsMapping = if (folder.name in listOf("custom_scalar_type", "input_object_type", "mutation_create_review")) {
        mapOf("Date" to "java.util.Date", "URL" to "java.lang.String", "ID" to "java.lang.Integer")
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
          override fun apply(operationDocument: String, operationFilepath: String): String {
            return "hash"
          }

          override val version: String = "1"
        }
        else -> OperationIdGenerator.Sha256()
      }

      val enumAsSealedClassPatternFilters = when (folder.name) {
        "arguments_complex" -> setOf(".*") // test all pattern matching
        "arguments_simple" -> setOf("Bla-bla", "Yada-yada", "Ep.*de") // test multiple pattern matching
        "enum_type" -> setOf("Bla") // test not matching
        else -> emptySet()
      }

      val schemaFile = folder.listFiles()!!.find { it.isFile && it.name == "schema.sdl" }
          ?: File("src/test/graphql/schema.sdl")

      val graphqlFiles = setOf(File(folder, "TestOperation.graphql"))
      val operationOutputGenerator = OperationOutputGenerator.DefaultOperationOuputGenerator(operationIdGenerator)

      return GraphQLCompiler.Arguments(
          rootPackageName = "com.example.${folder.name}",
          rootFolders = listOf(folder),
          graphqlFiles = graphqlFiles,
          schemaFile = schemaFile,
          outputDir = File("build/generated/test/${folder.name}"),
          operationOutputGenerator = operationOutputGenerator,
          customScalarsMapping = customScalarsMapping,
          generateKotlinModels = true,
          useSemanticNaming = useSemanticNaming,
          generateAsInternal = generateAsInternal,
          generateFilterNotNull = true,
          enumAsSealedClassPatternFilters = enumAsSealedClassPatternFilters,
          metadataOutputFile = File("build/generated/test/${folder.name}/metadata"),
          dumpIR = false,

      )
    }

    @JvmStatic
    @Parameterized.Parameters(name = "{0}")
    fun data(): Collection<*> {
      val filterRegex = System.getProperty("codegenTests")?.takeIf { it.isNotEmpty() }?.trim()?.let { Regex(it) }
      return File("src/test/graphql/com/example/")
          .listFiles()!!
          .sortedBy {
            it.name
          }
          .filter { file ->
            /**
             * This allows to run a specific test from the command line by using something like:
             *
             * ./gradlew :apollo-compiler:test -DcodegenTests="fragments_with_type_condition" --tests '*Codegen*'
             */
            file.isDirectory && (filterRegex == null || filterRegex.matchEntire(file.name) != null)
          }
    }
  }
}
