package com.apollostack.compiler

import com.apollostack.compiler.ir.QueryIntermediateRepresentation
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec
import com.squareup.moshi.Moshi
import java.io.File

open class GraphqlCompiler {
  private val moshi = Moshi.Builder().build()

  fun write(relativePath: String): Status {
    val irAdapter = moshi.adapter(QueryIntermediateRepresentation::class.java)
    val ir = irAdapter.fromJson(File(relativePath).readText())
    // TODO: Handle multiple or no operations
    val operation = ir.operations.first()
    val customTypeCollector = CustomTypeCollector(operation.fields)
    val typeSpec = TypeSpec.interfaceBuilder(operation.operationName)
        .addMethods(customTypeCollector.collectMethods())
        .addTypes(customTypeCollector.collectTypes())
        .build()
    JavaFile.builder("test", typeSpec).build()
        .writeTo(OUTPUT_DIRECTORY.fold(File("build"), ::File))
    return Status.Success()
  }

  companion object {
    const val FILE_EXTENSION = "graphql"
    val OUTPUT_DIRECTORY = listOf("generated", "source", "apollo")
  }
}