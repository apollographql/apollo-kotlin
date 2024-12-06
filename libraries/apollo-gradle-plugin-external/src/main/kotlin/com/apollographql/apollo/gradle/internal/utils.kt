package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.compiler.InputFile
import org.gradle.api.file.FileCollection
import java.io.File


internal fun FileCollection.toInputFiles(): List<InputFile> {
  val inputFiles = mutableListOf<InputFile>()

  asFileTree.visit {
    if (it.file.isFile) {
      inputFiles.add(InputFile(it.file, it.path))
    }
  }

  return inputFiles
}

/**
 * Isolates inputs so we can use them from a separate classloader.
 *
 * See also https://github.com/gradle/gradle/issues/28147 for why this is needed.
 */
internal fun FileCollection.isolate(): List<Any> = toInputFiles().isolate()
internal fun List<InputFile>.isolate(): List<Any> = flatMap { listOf(it.normalizedPath, it.file) }
