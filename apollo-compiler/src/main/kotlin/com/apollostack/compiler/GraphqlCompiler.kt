package com.apollostack.compiler

import com.apollostack.compiler.ir.Field
import com.apollostack.compiler.ir.QueryIntermediateRepresentation
import com.squareup.javapoet.*
import com.squareup.moshi.Moshi
import java.io.File
import javax.lang.model.element.Modifier

open class GraphqlCompiler {
  private val moshi = Moshi.Builder().build()

  fun write(relativePath: String): Status {
    val irAdapter = moshi.adapter(QueryIntermediateRepresentation::class.java)
    val ir = irAdapter.fromJson(File(relativePath).readText())
    // TODO: Handle multiple or no operations
    val operation = ir.operations.first()
    val methods = operation.fields.map { it.toMethodSpec() }
    val customTypes = operation.fields.filter { it.fields.any() }.distinct().map {
      // TODO: Also need to recurse into inner types
      TypeSpec.interfaceBuilder(it.type)
          .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
          .addMethods(it.fields.map { it.toMethodSpec() })
          .build()
    }
    val typeSpec = TypeSpec.interfaceBuilder(operation.operationName)
        .addMethods(methods)
        .addTypes(customTypes)
        .build()
    JavaFile.builder("test", typeSpec).build()
        .writeTo(OUTPUT_DIRECTORY.fold(File("build"), ::File))
    return Status.Success()
  }

  private fun Field.toMethodSpec() =
      MethodSpec.methodBuilder(responseName)
          .returns(type.toTypeName())
          .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
          .build()

  private fun String.toTypeName(): TypeName =
      // TODO: Handle other primitive types
      if (this == "String!") {
        ClassName.get(String::class.java)
      } else {
        ClassName.get("", this)
      }

  companion object {
    const val FILE_EXTENSION = "graphql"
    val OUTPUT_DIRECTORY = listOf("generated", "source", "apollo")
  }
}