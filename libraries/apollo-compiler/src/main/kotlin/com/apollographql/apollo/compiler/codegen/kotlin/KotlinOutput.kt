package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.compiler.CodegenMetadata
import com.apollographql.apollo.compiler.codegen.SourceFile
import com.apollographql.apollo.compiler.codegen.SourceOutput
import com.squareup.kotlinpoet.FileSpec
import java.io.OutputStream

/**
 * Output of the Kotlin code generation. It's a list of [kotlinpoet](https://square.github.io/kotlinpoet/) [FileSpec]
 * together with some metadata that maps the Kotlin files to their GraphQL origin.
 */
class KotlinOutput(
    val fileSpecs: List<FileSpec>,
    val codegenMetadata: CodegenMetadata,
) {
  operator fun plus(other: KotlinOutput): KotlinOutput {
    return KotlinOutput(
        fileSpecs + other.fileSpecs,
        codegenMetadata + other.codegenMetadata
    )
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
      this.codegenMetadata
  )
}


