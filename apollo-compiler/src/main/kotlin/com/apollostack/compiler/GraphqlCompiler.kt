package com.apollostack.compiler

import com.squareup.javapoet.*
import java.io.File
import javax.lang.model.element.Modifier

open class GraphqlCompiler {
  fun write(relativePath: String): Status {
    val typeSpec = TypeSpec.interfaceBuilder("TwoHeroes")
        .addType(TypeSpec.interfaceBuilder("Character")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addMethod(MethodSpec.methodBuilder("name")
                .returns(TypeName.get(String::class.java))
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .build())
            .build())
        .addMethod(MethodSpec.methodBuilder("k2")
            .returns(ClassName.get("", "Character"))
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .build())
        .addMethod(MethodSpec.methodBuilder("luke")
            .returns(ClassName.get("", "Character"))
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .build())
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