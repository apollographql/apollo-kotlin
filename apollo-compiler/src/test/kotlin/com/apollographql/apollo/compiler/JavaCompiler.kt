package com.apollographql.apollo.compiler

import com.google.common.truth.Truth
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourcesSubjectFactory
import java.io.File

object JavaCompiler {
  fun assertCompiles(files: Set<File>) {
    val javaFileObjects = files.map {
      val qualifiedName = it.path
          .substringBeforeLast(".")
          .split(File.separator)
          .joinToString(".")

      JavaFileObjects.forSourceLines(qualifiedName,
          it.readLines())
    }.toList()

    Truth.assertAbout(JavaSourcesSubjectFactory.javaSources()).that(javaFileObjects).compilesWithoutError()
  }
}