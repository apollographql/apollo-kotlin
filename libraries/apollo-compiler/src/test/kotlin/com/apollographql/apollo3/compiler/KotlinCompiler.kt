package com.apollographql.apollo3.compiler

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import okio.buffer
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Assert
import java.io.File

object KotlinCompiler {
  @OptIn(ExperimentalCompilerApi::class)
  fun assertCompiles(
      files: Set<File>,
  ) {
    val kotlinFiles = files.map {
      SourceFile.fromPath(it)
    }.toList()

    val result = KotlinCompilation().apply {
      sources = kotlinFiles

      kotlincArguments = kotlincArguments + "-opt-in=kotlin.RequiresOptIn"
      inheritClassPath = true
      verbose = false
      messageOutputStream = okio.blackholeSink().buffer().outputStream()
    }.compile()

    if (result.exitCode != KotlinCompilation.ExitCode.OK) {
      Assert.fail("Failed to compile generated Kotlin files due to compiler errors: ${result.messages}")
    }
  }
}
