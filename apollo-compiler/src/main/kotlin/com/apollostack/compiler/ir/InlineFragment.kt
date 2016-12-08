package com.apollostack.compiler.ir

import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

data class InlineFragment(
    val typeCondition: String,
    val fields: List<Field>
) : CodeGenerator {

  override fun toTypeSpec(): TypeSpec =
      TypeSpec.interfaceBuilder(interfaceName())
          .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
          .addMethods(scalarFields().map(Field::toMethodSpec))

          .build()

  fun interfaceName() = "$INTERFACE_PREFIX${typeCondition.capitalize()}"

  private fun scalarFields() = fields.filter(Field::isScalar)

  companion object {
    private val INTERFACE_PREFIX = "As"
  }

}