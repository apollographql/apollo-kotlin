package com.apollographql.apollo3.compiler.codegen.java

import com.apollographql.apollo3.compiler.CodegenMetadata
import com.apollographql.apollo3.compiler.codegen.SourceFile
import com.apollographql.apollo3.compiler.codegen.SourceOutput
import com.squareup.javapoet.JavaFile
import java.io.OutputStream


class JavaOutput(
    val javaFiles: List<JavaFile>,
    val codegenMetadata: CodegenMetadata,
) {
  operator fun plus(other: JavaOutput): JavaOutput {
    return JavaOutput(
        javaFiles + other.javaFiles,
        codegenMetadata + other.codegenMetadata
    )
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
      this.codegenMetadata
  )
}
