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
    val fragmentsReferenced: List<String>) {
  /** Returns a Java method that returns the interface represented by this Fragment object. */
  private fun toMethodSpec() =
      MethodSpec.methodBuilder(fragmentName.decapitalize())
          .returns(ClassName.get("", fragmentName.capitalize()))
          .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
          .build()

  /** Returns the Java interface that represents this Fragment object. */
  fun toTypeSpec(): TypeSpec =
      TypeSpec.interfaceBuilder(fragmentName.capitalize())
          .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
          .addMethods(fields.map(Field::toMethodSpec))
          .build()

  companion object {
    private val INTERFACE_NAME = "Fragments"

    fun genericAccessorMethodSpec(): MethodSpec =
        MethodSpec.methodBuilder("fragments")
            .returns(ClassName.get("", INTERFACE_NAME))
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .build()

    /** Returns a generic `Fragments` interface with methods for each of the provided fragments */
    fun genericInterfaceSpec(fragments: List<Fragment>): TypeSpec =
        TypeSpec.interfaceBuilder(INTERFACE_NAME)
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addMethods(fragments.map(Fragment::toMethodSpec))
            .build()
  }
}