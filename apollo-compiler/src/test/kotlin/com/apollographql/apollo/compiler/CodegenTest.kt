package com.apollographql.apollo.compiler

import com.apollographql.apollo.compiler.operationoutput.OperationDescriptor
import com.apollographql.apollo.compiler.parser.graphql.GraphQLDocumentParser
import com.apollographql.apollo.compiler.parser.introspection.IntrospectionSchema
import com.apollographql.apollo.api.internal.QueryDocumentMinifier
import com.google.common.truth.Truth.assertAbout
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourcesSubjectFactory.javaSources
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.lang.Exception

@RunWith(Parameterized::class)
class CodeGenTest(private val folder: File) {
  @Test
  fun generateExpectedClasses() {
    generateExpectedClasses(arguments(folder = folder, generateKotlinModels = false))
    generateExpectedClasses(arguments(folder = folder, generateKotlinModels = true))
  }

  data class GeneratedFile(val expected: File, val actual: File, val relativePath: String)

  private fun generateExpectedClasses(args: GraphQLCompiler.Arguments) {
    args.outputDir.deleteRecursively()
    GraphQLCompiler().write(args)

    val extension = if (args.generateKotlinModels) {
      "kt"
    } else {
      "java"
    }

    val expectedRoot = folder.parentFile.parentFile.parentFile
    val expectedFiles = folder.walk().filter {
      it.isFile && it.extension == extension
    }

    val actualRoot = args.outputDir
    val actualFiles = actualRoot.walk().filter {
      // extension should always be the correct one, it's a bug else
      it.isFile
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
    if (!args.generateKotlinModels) {
      val javaFileObjects = actualFiles.map {
        val qualifiedName = it.path
            .substringBeforeLast(".")
            .split(File.separator)
            .joinToString(".")

        JavaFileObjects.forSourceLines(qualifiedName,
            it.readLines())
      }.toList()

      assertAbout(javaSources()).that(javaFileObjects).compilesWithoutError()
    } else {
      val kotlinFiles = actualFiles.map {
        SourceFile.kotlin(it.name, it.readText())
      }.toList()

      val result = KotlinCompilation().apply {
        jvmTarget = "1.8"
        sources = kotlinFiles

        val expectedWarnings = folder.name in listOf("deprecation", "custom_scalar_type_warnings", "arguments_complex", "arguments_simple")
        allWarningsAsErrors = false // TODO: enable again when kotlin-test-compile targets Kotlin 1.4 (was expectedWarnings.not())
        inheritClassPath = true
        messageOutputStream = System.out // see diagnostics in real time
      }.compile()

      if (result.exitCode != KotlinCompilation.ExitCode.OK) {
        val compilationErrorMessages = "\\ne: .*\\n+".toRegex().find(result.messages)?.groupValues ?: emptyList()
        val errorMessages = compilationErrorMessages.joinToString(prefix = "\n", separator = "\n", postfix = "\n") {
          "`${it.replace("\n", "")}`"
        }
        fail("Failed to compile generated Kotlin files due to compiler errors: $errorMessages")
      }
    }
  }

  companion object {
    fun arguments(folder: File, generateKotlinModels: Boolean): GraphQLCompiler.Arguments {
      val customTypeMap = if (folder.name in listOf("custom_scalar_type", "input_object_type", "mutation_create_review")) {
        if (generateKotlinModels) {
          mapOf("Date" to "java.util.Date", "URL" to "kotlin.String", "ID" to "kotlin.Int")
        } else {
          mapOf("Date" to "java.util.Date", "URL" to "java.lang.String", "ID" to "java.lang.Integer")
        }
      } else {
        emptyMap()
      }
      val nullableValueType = when (folder.name) {
        "hero_details_guava" -> NullableValueType.GUAVA_OPTIONAL
        "hero_details_java_optional" -> NullableValueType.JAVA_OPTIONAL
        "fragments_with_type_condition_nullable" -> NullableValueType.ANNOTATED
        "hero_details_nullable" -> NullableValueType.ANNOTATED
        "union_fragment" -> NullableValueType.ANNOTATED
        else -> NullableValueType.APOLLO_OPTIONAL
      }
      val useSemanticNaming = when (folder.name) {
        "hero_details_semantic_naming" -> true
        "mutation_create_review_semantic_naming" -> true
        else -> false
      }
      val generateModelBuilder = when (folder.name) {
        "fragment_with_inline_fragment" -> true
        else -> false
      }
      val useJavaBeansSemanticNaming = when (folder.name) {
        "java_beans_semantic_naming" -> true
        else -> false
      }
      val suppressRawTypesWarning = when (folder.name) {
        "custom_scalar_type_warnings" -> true
        else -> false
      }
      val generateVisitorForPolymorphicDatatypes = when (folder.name) {
        "java_beans_semantic_naming" -> false
        else -> true
      }
      val generateAsInternal = when (folder.name) {
        "mutation_create_review", "simple_fragment" -> true
        else -> false
      }

      val schemaJson = folder.listFiles()!!.find { it.isFile && it.name == "schema.json" }
          ?: File("src/test/graphql/schema.json")
      val schema = IntrospectionSchema(schemaJson)
      val graphQLFile = File(folder, "TestOperation.graphql")

      val packageNameProvider = DefaultPackageNameProvider(
          rootFolders = listOf(folder),
          schemaFile = schemaJson,
          rootPackageName = "com.example.${folder.name}"
      )

      val operationIdGenerator = when (folder.name) {
        "operation_id_generator" -> object : OperationIdGenerator {
          override fun apply(operationDocument: String, operationFilepath: String): String {
            return "hash"
          }

          override val version: String = "1"
        }
        else -> OperationIdGenerator.Sha256()
      }

      val enumAsSealedClassPatternFilters = when(folder.name) {
        "arguments_complex" -> listOf(".*") // test all pattern matching
        "arguments_simple" -> listOf("Bla-bla", "Yada-yada", "Ep.*de") // test multiple pattern matching
        "enum_type" -> listOf("Bla") // test not matching
        else -> emptyList()
      }

      val ir = GraphQLDocumentParser(schema, packageNameProvider).parse(setOf(graphQLFile))

      val operationOutput = ir.operations.map {
        operationIdGenerator.apply(QueryDocumentMinifier.minify(it.sourceWithFragments), it.filePath) to OperationDescriptor(
            name = it.operationName,
            packageName = it.packageName,
            filePath = it.filePath,
            source = QueryDocumentMinifier.minify(it.sourceWithFragments)
        )
      }.toMap()

      val language = if (generateKotlinModels) "kotlin" else "java"
      return GraphQLCompiler.Arguments(
          ir = ir,
          outputDir = File("build/generated/test/${folder.name}/$language"),
          operationOutput = operationOutput,
          customTypeMap = customTypeMap,
          generateKotlinModels = generateKotlinModels,
          nullableValueType = nullableValueType,
          useSemanticNaming = useSemanticNaming,
          generateModelBuilder = generateModelBuilder,
          useJavaBeansSemanticNaming = useJavaBeansSemanticNaming,
          suppressRawTypesWarning = suppressRawTypesWarning,
          generateVisitorForPolymorphicDatatypes = generateVisitorForPolymorphicDatatypes,
          generateAsInternal = generateAsInternal,
          kotlinMultiPlatformProject = true,
          enumAsSealedClassPatternFilters = enumAsSealedClassPatternFilters
      )
    }

    @JvmStatic
    @Parameterized.Parameters(name = "{0}")
    fun data(): Collection<File> {
      return File("src/test/graphql/com/example/")
          .listFiles()!!
          .filter { it.isDirectory }
    }

    private fun shouldUpdateTestFixtures(): Boolean {
      return when (System.getProperty("updateTestFixtures")?.trim()) {
        "on", "true", "1" -> true
        else -> false
      }
    }

    fun checkTestFixture(actual: File, expected: File) {
      val actualText = actual.readText()
      val expectedText = expected.readText()

      if (actualText != expectedText) {
        if (shouldUpdateTestFixtures()) {
          expected.writeText(actualText)
        } else {
          throw Exception("""generatedFile content doesn't match the expectedFile content.
      |If you changed the compiler recently, you need to update the testFixtures.
      |Run the tests with `-DupdateTestFixtures=true` to do so.
      |generatedFile: ${actual.path}
      |expectedFile: ${expected.path}""".trimMargin())
        }
      }
    }
  }
}
