package com.apollostack.compiler.ir

import com.apollostack.compiler.SchemaTypeSpecBuilder
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

data class InlineFragment(
    val typeCondition: String,
    val fields: List<Field>,
    val fragmentSpreads: List<String>?
) : CodeGenerator {
  override fun toTypeSpec(): TypeSpec =
    TypeSpec.interfaceBuilder(interfaceName())
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .addMethods(fields.map(Field::toMethodSpec))
        .addTypes(fields.filter(Field::isNonScalar).map { field ->
          SchemaTypeSpecBuilder().build(field.normalizedName(), field.fields ?: emptyList(), fragmentSpreads ?: emptyList(),
              field.inlineFragments ?: emptyList())
        })
        .build()

  fun interfaceName() = "$INTERFACE_PREFIX${typeCondition.capitalize()}"

  companion object {
    private val INTERFACE_PREFIX = "As"
  }
}