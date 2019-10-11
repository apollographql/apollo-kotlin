package com.apollographql.apollo.compiler

import com.apollographql.apollo.compiler.parser.GraphQLDocumentParser
import com.apollographql.apollo.compiler.parser.Schema
import com.google.common.truth.Truth.assertAbout
import com.google.common.truth.Truth.assertThat
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourcesSubjectFactory.javaSources
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.*
import javax.tools.JavaFileObject

@RunWith(Parameterized::class)
class CodeGenTest(val folder: File) {
  private val javaExpectedFileMatcher = FileSystems.getDefault().getPathMatcher("glob:**.java")
  private val kotlinExpectedFileMatcher = FileSystems.getDefault().getPathMatcher("glob:**.kt")
  private val sourceFileObjects: MutableList<JavaFileObject> = ArrayList()

  @Test
  fun generateExpectedClasses() {
    generateJavaExpectedClasses(arguments(folder = folder, generateKotlinModels = false))
    generateKotlinExpectedClasses(arguments(folder = folder, generateKotlinModels = true))
  }

  private fun generateJavaExpectedClasses(args: GraphQLCompiler.Arguments) {
    GraphQLCompiler().write(args)

    Files.walkFileTree(folder.toPath(), object : SimpleFileVisitor<Path>() {
      override fun visitFile(expectedFile: Path, attrs: BasicFileAttributes): FileVisitResult {
        if (javaExpectedFileMatcher.matches(expectedFile)) {
          val expected = expectedFile.toFile()

          val actualClassName = actualClassName(expectedFile, "java")
          val actual = findActual(actualClassName, "java", args)

          if (!actual.isFile) {
            throw AssertionError("Couldn't find actual file: $actual")
          }

          assertThat(actual.readText()).isEqualTo(expected.readText())
          sourceFileObjects.add(JavaFileObjects.forSourceLines("com.example.${folder.name}.$actualClassName",
              actual.readLines()))
        }
        return FileVisitResult.CONTINUE
      }
    })
    assertAbout(javaSources()).that(sourceFileObjects).compilesWithoutError()
  }

  private fun generateKotlinExpectedClasses(args: GraphQLCompiler.Arguments) {
    GraphQLCompiler().write(args)

    Files.walkFileTree(folder.toPath(), object : SimpleFileVisitor<Path>() {
      override fun visitFile(expectedFile: Path, attrs: BasicFileAttributes): FileVisitResult {
        if (kotlinExpectedFileMatcher.matches(expectedFile)) {
          val expected = expectedFile.toFile()

          val actualClassName = actualClassName(expectedFile, "kt")
          val actual = findActual(actualClassName, "kt", args)

          if (!actual.isFile) {
            throw AssertionError("Couldn't find actual file: $actual")
          }

          assertThat(actual.readText()).isEqualTo(expected.readText())
        }
        return FileVisitResult.CONTINUE
      }
    })
  }

  private fun actualClassName(expectedFile: Path, extension: String): String {
    return expectedFile.fileName.toString().replace("Expected", "").replace(".$extension", "")
  }

  private fun findActual(className: String, extension: String, args: GraphQLCompiler.Arguments): File {
    val possiblePaths = arrayOf("$className.$extension", "type/$className.$extension", "fragment/$className.$extension")
    possiblePaths
        .map { args.outputDir.toPath().resolve("com/example/${folder.name}/$it").toFile() }
        .filter { it.isFile }
        .forEach { return it }
    throw AssertionError("Couldn't find actual file: $className")
  }

  companion object {
    fun arguments(folder: File, generateKotlinModels: Boolean): GraphQLCompiler.Arguments {
      val customTypeMap = if (folder.name in listOf("custom_scalar_type", "input_object_type",
              "mutation_create_review")) {
        mapOf("Date" to "java.util.Date", "URL" to "java.lang.String", "ID" to "java.lang.Integer")
      } else {
        emptyMap()
      }
      val nullableValueType = when {
        folder.name == "hero_details_guava" -> NullableValueType.GUAVA_OPTIONAL
        folder.name == "hero_details_java_optional" -> NullableValueType.JAVA_OPTIONAL
        folder.name == "fragments_with_type_condition_nullable" -> NullableValueType.ANNOTATED
        folder.name == "hero_details_nullable" -> NullableValueType.ANNOTATED
        folder.name == "union_fragment" -> NullableValueType.ANNOTATED
        else -> NullableValueType.APOLLO_OPTIONAL
      }
      val useSemanticNaming = when {
        folder.name == "hero_details_semantic_naming" -> true
        folder.name == "mutation_create_review_semantic_naming" -> true
        else -> false
      }
      val generateModelBuilder = when {
        folder.name == "fragment_with_inline_fragment" -> true
        else -> false
      }
      val useJavaBeansSemanticNaming = when {
        folder.name == "java_beans_semantic_naming" -> true
        else -> false
      }
      val suppressRawTypesWarning = when {
        folder.name == "custom_scalar_type_warnings" -> true
        else -> false
      }
      val generateVisitorForPolymorphicDatatypes = when {
        folder.name == "java_beans_semantic_naming" -> false
        else -> true
      }

      val schemaJson = folder.listFiles().find { it.isFile && it.name == "schema.json" }
          ?: File("src/test/graphql/schema.json")
      val schema = Schema(schemaJson)
      val graphQLFile = File(folder, "TestOperation.graphql")

      val outputPackageName = "com.example.${folder.name}"

      val packageNameProvider = PackageNameProvider(
          rootPackageName = "",
          schemaPackageName = "",
          outputPackageName = outputPackageName
      )
      val ir = GraphQLDocumentParser(schema, packageNameProvider).parse(listOf(graphQLFile))
      val args = GraphQLCompiler.Arguments(
          ir = ir,
          outputDir = GraphQLCompiler.OUTPUT_DIRECTORY.plus("sources").fold(File("build"), ::File),
          customTypeMap = customTypeMap,
          generateKotlinModels = generateKotlinModels,
          nullableValueType = nullableValueType,
          useSemanticNaming = useSemanticNaming,
          generateModelBuilder = generateModelBuilder,
          useJavaBeansSemanticNaming = useJavaBeansSemanticNaming,
          suppressRawTypesWarning = suppressRawTypesWarning,
          generateVisitorForPolymorphicDatatypes = generateVisitorForPolymorphicDatatypes,
          packageNameProvider = packageNameProvider
      )
      return args
    }

    @JvmStatic
    @Parameterized.Parameters(name = "{0}")
    fun data(): Collection<File> {
      return File("src/test/graphql/com/example/")
          .listFiles()!!
          .filter { it.isDirectory }
          // TODO figure out what to do with these cases
          .filter { it.name != "nested_inline_fragment" && it.name != "reserved_words" && it.name != "scalar_types" }
    }
  }
}
