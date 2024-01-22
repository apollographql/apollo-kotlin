package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.compiler.codegen.CodegenSymbols
import com.apollographql.apollo3.compiler.ir.IrOperations
import java.io.File
import java.io.OutputStream

interface SourceFile {
  val packageName: String
  val name: String
  fun writeTo(outputStream: OutputStream)
}

class BuildOutput(
    val sourceOutput: SourceOutput,
    val irOperations: IrOperations
)
class SourceOutput(
    val files: List<SourceFile>,
    val symbols: CodegenSymbols
) {
  operator fun plus(other: SourceOutput): SourceOutput{
    return SourceOutput(
        files + other.files,
        symbols + other.symbols
    )
  }

  companion object {
    val Empty = SourceOutput(emptyList(), CodegenSymbols.Empty)
  }
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
      symbols.writeTo(codegenSymbolsFile)
    }
  }
}