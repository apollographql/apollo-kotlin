package com.apollostack.compiler.codegen

import com.apollostack.compiler.GraphQLCompiler
import com.google.common.truth.Truth.assertAbout
import com.google.common.truth.Truth.assertThat
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourcesSubjectFactory.javaSources
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.FileVisitResult
import java.nio.file.attribute.BasicFileAttributes
import java.util.ArrayList
import javax.tools.JavaFileObject

@RunWith(Parameterized::class)
class CodeGenTest(val testDir: File, val pkgName: String, val generatePOJO: Boolean) {
  private val testDirPath = testDir.toPath()
  private val expectedFileMatcher = FileSystems.getDefault().getPathMatcher("glob:**Expected.java")

  private val compiler = GraphQLCompiler()
  private val outputDir = GraphQLCompiler.OUTPUT_DIRECTORY.fold(File("build"), ::File)
  private val sourceFileObjects: MutableList<JavaFileObject> = ArrayList()

  @Test
  fun generateExpectedClasses() {
    val irFile = File(testDir, "TestQuery.json")
    compiler.write(irFile, outputDir, generatePOJO)

    Files.walkFileTree(testDirPath, object : SimpleFileVisitor<Path>() {
      override fun visitFile(expectedFile: Path, attrs: BasicFileAttributes): FileVisitResult {
        if (expectedFileMatcher.matches(expectedFile)) {
          val expected = expectedFile.toFile()

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
    for (p in possiblePaths) {
      val actual = outputDir.toPath().resolve("com/example/$pkgName/$p").toFile()
      if (actual.isFile) {
        return actual
      }
    }
    throw AssertionError("Couldn't find actual file: $className")
  }

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{1}")
    fun data(): Collection<Array<Any>> {
      return File("src/test/graphql/com/example/").listFiles()
          .filter { it.isDirectory }
          .map { arrayOf(it, it.name, it.name.startsWith("pojo")) }
    }
  }
}
