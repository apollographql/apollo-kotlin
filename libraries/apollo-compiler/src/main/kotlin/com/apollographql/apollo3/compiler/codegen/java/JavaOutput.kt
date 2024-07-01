package com.apollographql.apollo.compiler.codegen.java

import com.apollographql.apollo.compiler.CodegenMetadata
import com.apollographql.apollo.compiler.codegen.SourceFile
import com.apollographql.apollo.compiler.codegen.SourceOutput
import com.squareup.javapoet.JavaFile
import java.io.OutputStream


/**
 * Output of the Java code generation. It's a list of [javapoet](https://github.com/square/javapoet) [JavaFile]
 * together with some metadata that maps the Java files to their GraphQL origin.
 */
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
