package com.apollographql.apollo3.compiler

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.Assert
import java.io.File

object KotlinCompiler {
  fun assertCompiles(files: Set<File>, allWarningsAsErrors: Boolean) {
    val kotlinFiles = files.map {
      SourceFile.fromPath(it)
    }.toList()

    val result = KotlinCompilation().apply {
      sources = kotlinFiles

      this.allWarningsAsErrors = allWarningsAsErrors
      inheritClassPath = true
      verbose = true
    }.compile()

    if (result.exitCode != KotlinCompilation.ExitCode.OK) {
      Assert.fail("Failed to compile generated Kotlin files due to compiler errors: ${result.messages}")
    }
  }
}