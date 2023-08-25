package com.apollographql.apollo3.compiler

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import okio.buffer
import org.junit.Assert
import java.io.File

object KotlinCompiler {
  fun assertCompiles(
      files: Set<File>,
      allWarningsAsErrors: Boolean,

      /**
       * TODO: Remove this once kotlin-compile-testing with Kotlin 1.9 is released.
       */
      enableEnumEntriesLanguageFeature: Boolean,
  ) {
    val kotlinFiles = files.map {
      SourceFile.fromPath(it)
    }.toList()

    val result = KotlinCompilation().apply {
      sources = kotlinFiles

      kotlincArguments = kotlincArguments + "-opt-in=kotlin.RequiresOptIn"
      if (enableEnumEntriesLanguageFeature) {
        kotlincArguments = kotlincArguments + "-XXLanguage:+EnumEntries" + "-Xsuppress-version-warnings"
      }
      this.allWarningsAsErrors = allWarningsAsErrors
      inheritClassPath = true
      verbose = false
      messageOutputStream = okio.blackholeSink().buffer().outputStream()
      if (enableEnumEntriesLanguageFeature) languageVersion = "1.9"
    }.compile()

    if (result.exitCode != KotlinCompilation.ExitCode.OK) {
      Assert.fail("Failed to compile generated Kotlin files due to compiler errors: ${result.messages}")
    }
  }
}
