package com.apollographql.apollo.compiler.codegen

import com.apollographql.apollo.compiler.CodegenMetadata
import com.apollographql.apollo.compiler.writeTo
import java.io.File
import java.io.OutputStream

class SourceOutput(
    val files: List<SourceFile>,
    val codegenMetadata: CodegenMetadata
) {
  operator fun plus(other: SourceOutput): SourceOutput{
    return SourceOutput(
        files + other.files,
        codegenMetadata + other.codegenMetadata
    )
  }
}

infix fun SourceOutput?.plus(other: SourceOutput): SourceOutput {
  if (this == null) {
    return other
  }

  return this + other
}

fun SourceOutput.writeTo(directory: File?, deleteDirectoryFirst: Boolean, codegenSymbolsFile: File?) {
  if (directory != null) {
    if (deleteDirectoryFirst) {
      directory.deleteRecursively()
    }

    files.forEach { file ->
      file.packageName.split(".").plus(file.name).fold(directory) { acc, item -> acc.resolve(item) }.run {
        parentFile.mkdirs()
        outputStream().use {
          file.writeTo(it)
        }
      }
    }
    if (codegenSymbolsFile != null) {
      codegenMetadata.writeTo(codegenSymbolsFile)
    }
  }
}

interface SourceFile {
  val packageName: String
  val name: String
  fun writeTo(outputStream: OutputStream)
}

