package com.apollostack.compiler.ir

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

data class Fragment(
    val fragmentName: String,
    val source: String,
    val typeCondition: String,
    val fields: List<Field>,
    val fragmentsSpread: List<String>,
    val inlineFragments: List<String>,
    val fragmentsReferenced: List<String>
) : CodeGenerator {
  /** Returns a Java method that returns the interface represented by this Fragment object. */
  fun toMethodSpec(): MethodSpec =
      MethodSpec.methodBuilder(fragmentName.decapitalize())
          .returns(ClassName.get("", fragmentName.capitalize()))
          .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
          .build()

  /** Returns the Java interface that represents this Fragment object. */
  override fun toTypeSpec(fragments: List<Fragment>): TypeSpec =
      TypeSpec.interfaceBuilder(fragmentName.capitalize())
          .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
          .addMethods(fields.map(Field::toMethodSpec))
          .addTypes(fields.filter(Field::isNonScalar).map { it.toTypeSpec(fragments) })
          .build()
}