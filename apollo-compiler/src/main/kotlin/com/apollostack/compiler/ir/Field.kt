package com.apollostack.compiler.ir

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import javax.lang.model.element.Modifier

data class Field(
  val responseName: String,
  val fieldName: String,
  val type: String,
  val fields: List<Field>?
) {

  fun toTypeName(prefix:String? = null) = "${prefix?.capitalize() ?: ""}${type.normalizeTypeName()}"

  fun toMethodSpec(returnTypeOverride: String? = null): MethodSpec {
    val typeName = returnTypeOverride
      ?.let { if (type.startsWith('[') && type.endsWith(']')) "[${it.normalizeTypeName()}]" else it.normalizeTypeName() }
      ?.toTypeName()
      ?: type.toTypeName()

    return MethodSpec.methodBuilder(responseName)
      .returns(typeName)
      .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
      .build()
  }

  private fun String.toTypeName(): TypeName =
    // TODO: Handle other primitive types
    if (this == "String!") {
      ClassName.get(String::class.java)
    } else if (this == "ID!") {
      ClassName.LONG
    } else if (this == "Int") {
      TypeName.INT
    } else if (this.startsWith('[') && this.endsWith(']')) {
      ParameterizedTypeName.get(ClassName.get(List::class.java), ClassName.get("", this.normalizeTypeName()))
    } else {
      ClassName.get("", this)
    }

  private fun String.normalizeTypeName() = removeSuffix("!").removePrefix("[").removeSuffix("]").removeSuffix("!")
}
