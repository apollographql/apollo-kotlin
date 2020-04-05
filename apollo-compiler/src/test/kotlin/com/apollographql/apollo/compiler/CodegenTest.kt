package com.apollographql.apollo.compiler

import com.apollographql.apollo.compiler.parser.GraphQLDocumentParser
import com.apollographql.apollo.compiler.parser.Schema
import com.google.common.truth.Truth.assertAbout
import com.google.common.truth.Truth.assertThat
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourcesSubjectFactory.javaSources
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.lang.Exception
import javax.tools.JavaFileObject

@RunWith(Parameterized::class)
class CodeGenTest(val folder: File) {
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

    val walk = folder.walkTopDown()
    val files = walk.mapNotNull { file ->
      if (!file.name.endsWith(extension)) {
        return@mapNotNull null
      }

      val relativePath = "com/example/${folder.name}/${file.relativeTo(folder).path}"
      val actual = File(args.outputDir, relativePath)

      if (!actual.isFile) {
        throw AssertionError("Couldn't find actual file: $actual")
      }

      GeneratedFile(expected = file, actual = actual, relativePath = relativePath)
    }.toList()

    // Check that files match
    files.forEach {
      checkTestFixture(actual = it.actual, expected = it.expected)
    }

    // And that they compile
    if (!args.generateKotlinModels) {
      val javaFileObjects = files.map {
        val qualifiedName = it.relativePath
            .substringBeforeLast(".")
            .split(File.separator)
            .joinToString(".")

        JavaFileObjects.forSourceLines(qualifiedName,
            it.actual.readLines())
      }

      assertAbout(javaSources()).that(javaFileObjects).compilesWithoutError()
    } else {
      val kotlinFiles = files.map {
        SourceFile.kotlin(it.actual.name, it.actual.readText())
      }
      val result = KotlinCompilation().apply {
        jvmTarget = "1.8"
        sources = kotlinFiles

        inheritClassPath = true
        messageOutputStream = System.out // see diagnostics in real time
      }.compile()

      assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }
  }

  companion object {
    fun arguments(folder: File, generateKotlinModels: Boolean): GraphQLCompiler.Arguments {
      val customTypeMap = if (folder.name in listOf("custom_scalar_type", "input_object_type",
              "mutation_create_review")) {
        mapOf("Date" to "java.util.Date", "URL" to "java.lang.String", "ID" to "java.lang.Integer")
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
      val schema = Schema(schemaJson)
      val graphQLFile = File(folder, "TestOperation.graphql")

      val packageNameProvider = DefaultPackageNameProvider(
          rootFolders = listOf(folder.absolutePath),
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

      val ir = GraphQLDocumentParser(schema, packageNameProvider).parse(setOf(graphQLFile))
      val language = if (generateKotlinModels) "kotlin" else "java"
      val args = GraphQLCompiler.Arguments(
          ir = ir,
          outputDir = File("build/generated/test/${folder.name}/$language"),
          operationIdGenerator = operationIdGenerator,
          customTypeMap = customTypeMap,
          generateKotlinModels = generateKotlinModels,
          nullableValueType = nullableValueType,
          useSemanticNaming = useSemanticNaming,
          generateModelBuilder = generateModelBuilder,
          useJavaBeansSemanticNaming = useJavaBeansSemanticNaming,
          suppressRawTypesWarning = suppressRawTypesWarning,
          generateVisitorForPolymorphicDatatypes = generateVisitorForPolymorphicDatatypes,
          packageNameProvider = packageNameProvider,
          generateAsInternal = generateAsInternal
      )
      return args
    }

    @JvmStatic
    @Parameterized.Parameters(name = "{0}")
    fun data(): Collection<File> {
      return File("src/test/graphql/com/example/")
          .listFiles()!!
          .filter { it.isDirectory }
    }
  }
}

fun checkTestFixture(actual: File, expected: File) {
  check (actual.exists()) {
    "actual=$actual not found (expected=$expected)"
  }
  check (expected.exists()) {
    "expected=$expected not found (actual=$actual)"
  }

  val actualText = actual.readText()
  val expectedText = expected.readText()

  if (actualText != expectedText) {
    when (System.getProperty("updateTestFixtures")?.trim()) {
      "on", "true", "1" -> {
        expected.writeText(actualText)
      }
      else -> {
        throw Exception("""generatedFile content doesn't match the expectedFile content.
      |If you changed the compiler recently, you need to update the testFixtures.
      |Run the tests with `-DupdateTestFixtures=true` to do so.
      |generatedFile: ${actual.path}
      |expectedFile: ${expected.path}""".trimMargin())
      }
    }
  }
}
