package com.apollostack.compiler.ir

import com.apollostack.compiler.FieldTypeSpecBuilder
import com.cesarferreira.pluralize.singularize
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

data class Field(
    val responseName: String,
    val fieldName: String,
    val type: String,
    val fields: List<Field>?,
    val fragmentSpreads: List<String>?
) : CodeGenerator {
  override fun toTypeSpec(): TypeSpec =
      FieldTypeSpecBuilder().build(normalizedName(), fields ?: emptyList(), emptyList())

  fun toMethodSpec(): MethodSpec =
      MethodSpec.methodBuilder(responseName)
          .returns(toTypeName(methodResponseType()))
          .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
          .build()

  /** Filters all fragments to only the ones referenced by this Field */
  fun referencedFragments(allFragments: List<Fragment>) =
      allFragments.filter { fragmentSpreads?.contains(it.fragmentName) ?: false }

  private fun toTypeName(responseType: String): TypeName =
      GraphQlType.resolveByName(responseType).toJavaTypeName()

  fun normalizedName() = responseName.capitalize().singularize()

  private fun methodResponseType(): String {
    if (isNonScalar() || hasFragments()) {
      // For non scalar fields, we use the responseName as the method return type.
      // However, we need to also encode any extra information from the `type` field
      // eg, [lists], nonNulls!, [[nestedLists]], [nonNullLists]!, etc
      val normalizedName = normalizedName()
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

  fun hasFragments() = fragmentSpreads?.any() ?: false
}
