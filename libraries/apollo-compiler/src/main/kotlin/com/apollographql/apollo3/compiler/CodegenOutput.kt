package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.compiler.codegen.CodegenSymbols
import com.squareup.javapoet.JavaFile
import com.squareup.kotlinpoet.FileSpec
import java.io.OutputStream

class JavaOutput(
    val javaFiles: List<JavaFile>,
    val symbols: CodegenSymbols,
) {
  operator fun plus(other: JavaOutput): JavaOutput {
    return JavaOutput(
        javaFiles + other.javaFiles,
        symbols + other.symbols
    )
  }

  companion object {
    val Empty = JavaOutput(emptyList(), CodegenSymbols(emptyList()))
  }
}

fun JavaOutput.toSourceOutput(): SourceOutput {
  return SourceOutput(
      javaFiles.map { javaFile ->
        object : SourceFile {


          override val packageName: String
            get() = javaFile.packageName
          override val name: String
            get() = javaFile.typeSpec.name + ".java"

          override fun writeTo(outputStream: OutputStream) {
            outputStream.bufferedWriter().apply {
              javaFile.writeTo(this)
              flush()
            }
          }
        }
      },
      this.symbols
  )
}

class KotlinOutput(
    val fileSpecs: List<FileSpec>,
    val symbols: CodegenSymbols,
) {
  operator fun plus(other: KotlinOutput): KotlinOutput {
    return KotlinOutput(
        fileSpecs + other.fileSpecs,
        symbols + other.symbols
    )
  }

  companion object {
    val Empty = KotlinOutput(emptyList(), CodegenSymbols(emptyList()))
  }
}

fun KotlinOutput.toSourceOutput(): SourceOutput {
  return SourceOutput(
      fileSpecs.map { fileSpec ->
        object : SourceFile {


          override val packageName: String
            get() = fileSpec.packageName
          override val name: String
            get() = fileSpec.name + ".kt"

          override fun writeTo(outputStream: OutputStream) {
            outputStream.bufferedWriter().apply {
              fileSpec.writeTo(this)
              flush()
            }
          }
        }
      },
      this.symbols
  )
}


