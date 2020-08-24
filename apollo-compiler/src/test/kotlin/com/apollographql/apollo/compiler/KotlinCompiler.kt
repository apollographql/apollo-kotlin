package com.apollographql.apollo.compiler

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.Assert
import java.io.File

object KotlinCompiler {
  fun assertCompiles(files: Set<File>, allWarningAsErrors: Boolean) {
    val kotlinFiles = files.map {
      SourceFile.kotlin(it.name, it.readText())
    }.toList()

    val result = KotlinCompilation().apply {
      jvmTarget = "1.8"
      sources = kotlinFiles

      allWarningsAsErrors = false // TODO: enable again when kotlin-test-compile targets Kotlin 1.4 allWarningAsErrors
      inheritClassPath = true
      messageOutputStream = System.out // see diagnostics in real time
    }.compile()

    if (result.exitCode != KotlinCompilation.ExitCode.OK) {
      val compilationErrorMessages = "\\ne: .*\\n+".toRegex().find(result.messages)?.groupValues ?: emptyList()
      val errorMessages = compilationErrorMessages.joinToString(prefix = "\n", separator = "\n", postfix = "\n") {
        "`${it.replace("\n", "")}`"
      }
      Assert.fail("Failed to compile generated Kotlin files due to compiler errors: $errorMessages")
    }
  }
}