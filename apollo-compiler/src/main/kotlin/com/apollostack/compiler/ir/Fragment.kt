package com.apollostack.compiler.ir

import com.apollostack.compiler.InterfaceTypeSpecBuilder
import com.apollostack.compiler.resolveNestedTypeNameDuplication
import com.squareup.javapoet.TypeSpec

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
      InterfaceTypeSpecBuilder().build(fragmentName.capitalize(), fields, fragmentSpreads, inlineFragments)
          .resolveNestedTypeNameDuplication(emptyList())
}