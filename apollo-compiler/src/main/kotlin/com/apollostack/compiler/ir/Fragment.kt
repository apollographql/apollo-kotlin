package com.apollostack.compiler.ir

import com.apollostack.compiler.InterfaceTypeSpecBuilder
import com.apollostack.compiler.resolveNestedTypeNameDuplication
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.MethodSpec
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
  /** Returns a Java method that returns the interface represented by this Fragment object. */
  fun toMethodSpec(): MethodSpec =
      MethodSpec.methodBuilder(fragmentName.decapitalize())
          .returns(ClassName.get("", fragmentName.capitalize()))
          .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
          .build()

  /** Returns the Java interface that represents this Fragment object. */
  override fun toTypeSpec(): TypeSpec =
      InterfaceTypeSpecBuilder().build(fragmentName.capitalize(), fields, fragmentSpreads, inlineFragments)
          .resolveNestedTypeNameDuplication(emptyList())
}