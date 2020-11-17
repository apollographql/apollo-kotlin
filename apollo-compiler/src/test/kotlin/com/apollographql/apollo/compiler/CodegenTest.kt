package com.apollographql.apollo.compiler

import com.apollographql.apollo.api.internal.QueryDocumentMinifier
import com.apollographql.apollo.compiler.TestUtils.checkTestFixture
import com.apollographql.apollo.compiler.TestUtils.shouldUpdateTestFixtures
import com.apollographql.apollo.compiler.parser.sdl.GraphSdlSchema
import com.apollographql.apollo.compiler.parser.sdl.toIntrospectionSchema
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

@RunWith(Parameterized::class)
class CodegenTest(private val folder: File, private val testLanguage: TestLanguage) {
  enum class TestLanguage {
    Java,
    Kotlin
  }

  @Test
  fun generateExpectedClasses() {
    val args = arguments(folder = folder, generateKotlinModels = testLanguage == TestLanguage.Kotlin)
    generateExpectedClasses(args)
  }

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
          println("removing stale expected file: ${expected.absolutePath}")
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
      JavaCompiler.assertCompiles(actualFiles.toSet())
    } else {
      val expectedWarnings = folder.name in listOf("deprecation", "custom_scalar_type_warnings", "arguments_complex", "arguments_simple")
      KotlinCompiler.assertCompiles(actualFiles.toSet(), !expectedWarnings)
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
        "arguments_complex" -> setOf(".*") // test all pattern matching
        "arguments_simple" -> setOf("Bla-bla", "Yada-yada", "Ep.*de") // test multiple pattern matching
        "enum_type" -> setOf("Bla") // test not matching
        else -> emptySet()
      }

      val packageName = when(folder.name) {
        // TODO reorganize tests so that we don't have to make this a child of "com.example.fragment_package_name"
        "fragment_package_name" -> "com.example.fragment_package_name.another"
        else -> null
      }

      val schemaFile = folder.listFiles()!!.find { it.isFile && it.name == "schema.sdl" }
          ?: File("src/test/graphql/schema.sdl")
      
      val graphqlFiles = setOf(File(folder, "TestOperation.graphql"))
      val operationOutputGenerator = OperationOutputGenerator.DefaultOperationOuputGenerator(operationIdGenerator)

      val language = if (generateKotlinModels) "kotlin" else "java"
      return GraphQLCompiler.Arguments(
          rootPackageName = "com.example.${folder.name}",
          rootFolders = listOf(folder),
          graphqlFiles = graphqlFiles,
          schemaFile = schemaFile,
          outputDir = File("build/generated/test/${folder.name}/$language"),
          operationOutputGenerator = operationOutputGenerator,
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
          enumAsSealedClassPatternFilters = enumAsSealedClassPatternFilters,
          metadataOutputFile = File("build/generated/test/${folder.name}/metadata/$language"),
          packageName = packageName
      )
    }

    @JvmStatic
    @Parameterized.Parameters(name = "{0}-{1}")
    fun data() =  File("src/test/graphql/com/example/")
          .listFiles()!!
          .filter { it.isDirectory }
          .let {
            it.map {
              arrayOf(it, TestLanguage.Kotlin)
            } + it.filter {
              // TODO: This specific test currently doesn't generate valid Java code
              // see https://github.com/apollographql/apollo-android/issues/2676
              !it.name.contains("test_inline")
            }.map {
              arrayOf(it, TestLanguage.Java)
            }
          }
  }
}
