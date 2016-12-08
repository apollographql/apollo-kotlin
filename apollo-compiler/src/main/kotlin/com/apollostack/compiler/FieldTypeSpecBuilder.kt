package com.apollostack.compiler

import com.apollostack.compiler.ir.Field
import com.apollostack.compiler.ir.Fragment
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

class FieldTypeSpecBuilder {
  fun build(typeName: String, fields: List<Field>, fragments: List<Fragment>): TypeSpec =
      TypeSpec.interfaceBuilder(typeName)
          .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
          .addMethods(fields.map(Field::toMethodSpec))
          .addTypes(innerTypes(fields, fragments))
          .addTypes(innerFragments(fields, fragments))
          .build()

  /** Returns a list of fragment types referenced by the provided list of fields */
  private fun innerFragments(fields: List<Field>, fragments: List<Fragment>): List<TypeSpec> =
      fields.filter(Field::hasFragments)
          .map { field ->
            // TODO: This is generating the same "Fragments" interface for every field that
            // references a fragment.
            TypeSpec.interfaceBuilder(field.normalizedName())
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addMethod(newFragmentAccessorMethodSpec())
                .addType(newFragmentInterfaceSpec(fragments))
                .build()
          }

  /** Returns a list of types referenced by the inner fields in the provided fields */
  private fun innerTypes(fields: List<Field>, fragments: List<Fragment>): List<TypeSpec> =
      fields.filter(Field::isNonScalar)
          .map { build(it.normalizedName(), it.fields!!, it.referencedFragments(fragments)) }

  companion object {
    private val FRAGMENTS_INTERFACE_NAME = "Fragments"

    private fun newFragmentAccessorMethodSpec(): MethodSpec =
        MethodSpec.methodBuilder("fragments")
            .returns(ClassName.get("", FRAGMENTS_INTERFACE_NAME))
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .build()

    /** Returns a generic `Fragments` interface with methods for each of the provided fragments */
    private fun newFragmentInterfaceSpec(fragments: List<Fragment>): TypeSpec =
        TypeSpec.interfaceBuilder(FRAGMENTS_INTERFACE_NAME)
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addMethods(fragments.map(Fragment::toMethodSpec))
            .build()
  }
}