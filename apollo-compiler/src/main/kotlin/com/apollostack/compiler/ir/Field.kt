package com.apollostack.compiler.ir

import com.cesarferreira.pluralize.singularize
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeName
import javax.lang.model.element.Modifier

data class Field(
    val responseName: String,
    val fieldName: String,
    val type: String,
    val fields: List<Field>?) {
  fun toMethodSpec(): MethodSpec {
    return MethodSpec.methodBuilder(responseName)
        .returns(toTypeName(methodResponseType()))
        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
        .build()
  }

  private fun toTypeName(responseType: String): TypeName =
      GraphQlType.resolveByName(responseType).toJavaTypeName()

  private fun methodResponseType(): String {
    if (isNonScalar()) {
      // For non scalar fields, we use the responseName as the method return type.
      // However, we need to also encode any extra information from the `type` field
      // eg, [lists], nonNulls!, [[nestedLists]], [nonNullLists]!, etc
      val normalizedName = responseName.capitalize().singularize()
      if (type.startsWith("[")) {
        // array type
        return if (type.endsWith("!")) "[$normalizedName]!" else "[$normalizedName]"
      } else if (type.endsWith("!")) {
        // non-null type
        return "$normalizedName!"
      } else {
        // nullable type
        return normalizedName
      }
    } else {
      return type
    }
  }

  fun isNonScalar() = fields?.any() ?: false
}
