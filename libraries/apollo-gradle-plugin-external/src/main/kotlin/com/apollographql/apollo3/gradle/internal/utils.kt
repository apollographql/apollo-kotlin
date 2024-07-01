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
 * See https://github.com/gradle/gradle/issues/28147 for why this is needed
 */
internal fun FileCollection.isolate(): List<Pair<String, File>> = toInputFiles().isolate()
internal fun List<InputFile>.isolate(): List<Pair<String, File>> = map { it.normalizedPath to it.file }
internal fun List<Pair<String, File>>.toInputFiles(): List<InputFile> = map { InputFile(it.second, it.first) }