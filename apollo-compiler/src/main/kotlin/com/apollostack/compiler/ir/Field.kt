package com.apollostack.compiler.ir

import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeName
import javax.lang.model.element.Modifier

data class Field(
  val responseName: String,
  val fieldName: String,
  val type: String,
  val fields: List<Field>?
) {

  fun toTypeName(prefix: String? = null) = "${prefix?.capitalize() ?: ""}${type.normalizeTypeName()}"

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

  private fun String.toTypeName(): TypeName = GraphQlType.resolveByName(this).toJavaTypeName()

  private fun String.normalizeTypeName() = removeSuffix("!").removeSurrounding(prefix = "[", suffix = "]").removeSuffix("!")
}
