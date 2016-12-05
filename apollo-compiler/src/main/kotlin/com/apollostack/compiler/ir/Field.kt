package com.apollostack.compiler.ir

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeName
import javax.lang.model.element.Modifier

data class Field(
    val responseName: String,
    val fieldName: String,
    val type: String,
    val fields: List<Field>) {

  fun toTypeName() = "${responseName.capitalize()}$type"

  fun toMethodSpec(returnTypeOverride: String? = null): MethodSpec =
      MethodSpec.methodBuilder(responseName)
          .returns(returnTypeName(returnTypeOverride))
          .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
          .build()

  fun returnTypeName(returnTypeOverride: String? = null): TypeName =
      // TODO: Handle other primitive types
      if (returnTypeOverride != null) {
        ClassName.get("", returnTypeOverride)
      } else if (type == "String!") {
        ClassName.get(String::class.java)
      } else if (type == "ID!") {
        ClassName.LONG
      } else {
        ClassName.get("", toTypeName())
      }
}
