package com.apollostack.compiler.ir

import com.apollostack.compiler.ClassNames
import com.apollostack.compiler.InterfaceTypeSpecBuilder
import com.apollostack.compiler.resolveNestedTypeNameDuplication
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

data class Fragment(
    val fragmentName: String,
    val source: String,
    val typeCondition: String,
    val fields: List<Field>,
    val fragmentSpreads: List<String>,
    val inlineFragments: List<InlineFragment>,
    val fragmentsReferenced: List<String>
) : CodeGenerator {
  /** Returns the Java interface that represents this Fragment object. */
  override fun toTypeSpec(): TypeSpec =
      InterfaceTypeSpecBuilder().build(interfaceTypeName(), fields, fragmentSpreads, inlineFragments)
          .toBuilder()
          .addFragmentDefinition(this)
          .build()
          .resolveNestedTypeNameDuplication(emptyList())

  fun interfaceTypeName() = fragmentName.capitalize()

  private fun TypeSpec.Builder.addFragmentDefinition(fragment: Fragment): TypeSpec.Builder {
    return addField(FieldSpec.builder(ClassNames.STRING, FRAGMENT_DEFINITION_FIELD_NAME)
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .initializer("\$S", fragment.source)
        .build()
    )
  }

  companion object {
    val FRAGMENT_DEFINITION_FIELD_NAME = "FRAGMENT_DEFINITION"
  }
}