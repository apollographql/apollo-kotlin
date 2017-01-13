package com.apollostack.compiler.ir

import com.apollostack.compiler.ClassNames
import com.apollostack.compiler.SchemaTypeSpecBuilder
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
  override fun toTypeSpec(abstract: Boolean): TypeSpec =
      SchemaTypeSpecBuilder(interfaceTypeName(), fields, fragmentSpreads, inlineFragments, abstract)
          .build(Modifier.PUBLIC)
          .toBuilder()
          .addFragmentDefinitionField()
          .addTypeConditionField()
          .build()
          .resolveNestedTypeNameDuplication(emptyList())

  fun interfaceTypeName() = fragmentName.capitalize()

  private fun TypeSpec.Builder.addFragmentDefinitionField(): TypeSpec.Builder =
      addField(FieldSpec.builder(ClassNames.STRING, FRAGMENT_DEFINITION_FIELD_NAME)
          .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
          .initializer("\$S", source)
          .build()
      )

  private fun TypeSpec.Builder.addTypeConditionField(): TypeSpec.Builder =
      addField(FieldSpec.builder(ClassNames.STRING, TYPE_CONDITION_FIELD_NAME)
          .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
          .initializer("\$S", typeCondition)
          .build()
      )

  companion object {
    val FRAGMENT_DEFINITION_FIELD_NAME: String = "FRAGMENT_DEFINITION"
    val TYPE_CONDITION_FIELD_NAME: String = "TYPE_CONDITION"
  }
}