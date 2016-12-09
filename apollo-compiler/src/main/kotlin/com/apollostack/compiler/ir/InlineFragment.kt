package com.apollostack.compiler.ir

import com.apollostack.compiler.FieldTypeSpecBuilder
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

data class InlineFragment(
    val typeCondition: String,
    val fields: List<Field>
) : CodeGenerator {
  override fun toTypeSpec(fragments: List<Fragment>): TypeSpec =
    TypeSpec.interfaceBuilder(interfaceName())
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .addMethods(fields.map(Field::toMethodSpec))
        .addTypes(fields.filter(Field::isNonScalar).map { field ->
          FieldTypeSpecBuilder().build(field.normalizedName(), field.fields ?: emptyList(), fragments,
              field.inlineFragments ?: emptyList())
        })
        .build()

  fun interfaceName() = "$INTERFACE_PREFIX${typeCondition.capitalize()}"

  companion object {
    private val INTERFACE_PREFIX = "As"
  }
}