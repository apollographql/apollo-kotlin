package com.apollographql.apollo.compiler

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
class CodeGenTest(val pkgName: String, val args: GraphQLCompiler.Arguments) {
  private val expectedFileMatcher = FileSystems.getDefault().getPathMatcher("glob:**.java")
  private val sourceFileObjects: MutableList<JavaFileObject> = ArrayList()

  @Test
  fun generateExpectedClasses() {
    GraphQLCompiler().write(args)
    Files.walkFileTree(args.irFile.parentFile.toPath(), object : SimpleFileVisitor<Path>() {
      override fun visitFile(expectedFile: Path, attrs: BasicFileAttributes): FileVisitResult {
        if (expectedFileMatcher.matches(expectedFile)) {
          val expected = expectedFile.toFile()

          System.out.print(expectedFile.fileName)
          val actualClassName = actualClassName(expectedFile)
          val actual = findActual(actualClassName)

          if (!actual.isFile) {
            throw AssertionError("Couldn't find actual file: $actual")
          }

          assertThat(actual.readText()).isEqualTo(expected.readText())
          sourceFileObjects.add(JavaFileObjects.forSourceLines("com.example.$pkgName.$actualClassName",
              actual.readLines()))
        }
        return FileVisitResult.CONTINUE
      }
    })
    assertAbout(javaSources()).that(sourceFileObjects).compilesWithoutError()
  }

  private fun actualClassName(expectedFile: Path): String {
    return expectedFile.fileName.toString().replace("Expected", "").replace(".java", "")
  }

  private fun findActual(className: String): File {
    val possiblePaths = arrayOf("$className.java", "type/$className.java", "fragment/$className.java")
    possiblePaths
        .map { args.outputDir.toPath().resolve("com/example/$pkgName/$it").toFile() }
        .filter { it.isFile }
        .forEach { return it }
    throw AssertionError("Couldn't find actual file: $className")
  }

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{0}")
    fun data(): Collection<Array<Any>> {
      return File("src/test/graphql/com/example/").listFiles()
          .filter { it.isDirectory }
          .map {
            val customTypeMap = if (it.name == "custom_scalar_type") {
              mapOf("Date" to "java.util.Date", "URL" to "java.lang.String")
            } else {
              emptyMap()
            }
            val nullableValueType = when {
              it.name == "hero_details_guava" -> NullableValueType.GUAVA_OPTIONAL
              it.name == "hero_details_java_optional" -> NullableValueType.JAVA_OPTIONAL
              (it.name != "hero_details_nullable" || it.name == "no_accessors") -> NullableValueType.APOLLO_OPTIONAL
              else -> NullableValueType.ANNOTATED
            }
            val useSemanticNaming = when {
              it.name == "hero_details_semantic_naming" -> true
              it.name == "mutation_create_review_semantic_naming" -> true
              else -> false
            }
            val generateAccessors = (it.name != "no_accessors")
            val args = GraphQLCompiler.Arguments(
                irFile = File(it, "TestOperation.json"),
                outputDir = GraphQLCompiler.Companion.OUTPUT_DIRECTORY.fold(File("build"), ::File),
                customTypeMap = customTypeMap,
                nullableValueType = nullableValueType,
                generateAccessors = generateAccessors,
                useSemanticNaming = useSemanticNaming)
            arrayOf(it.name, args)
          }
    }
  }
}
