package com.apollostack.compiler

import com.apollostack.compiler.ir.Field
import com.apollostack.compiler.ir.InlineFragment
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

class InterfaceTypeSpecBuilder {
  fun build(typeName: String, fields: List<Field>, fragmentSpreads: List<String>,
      inlineFragments: List<InlineFragment>): TypeSpec =
      TypeSpec.interfaceBuilder(typeName)
          .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
          .addFieldAccessorMethods(fields)
          .addInlineFragmentAccessorMethods(inlineFragments)
          .addInnerTypes(fields)
          .addInnerFragmentTypes(fragmentSpreads)
          .addInlineFragmentTypes(inlineFragments)
          .build()

  private fun TypeSpec.Builder.addFieldAccessorMethods(fields: List<Field>): TypeSpec.Builder {
    val methodSpecs = fields.map(Field::toMethodSpec)
    return addMethods(methodSpecs)
  }

  /** Returns a list of fragment types referenced by the provided list of fields */
  private fun TypeSpec.Builder.addInnerFragmentTypes(fragments: List<String>): TypeSpec.Builder {
    if (fragments.isNotEmpty()) {
      addMethod(newFragmentAccessorMethodSpec())
      addType(newFragmentInterfaceSpec(fragments))
    }
    return this
  }

  /** Returns a list of types referenced by the inner fields in the provided fields */
  private fun TypeSpec.Builder.addInnerTypes(fields: List<Field>): TypeSpec.Builder {
    val typeSpecs = fields.filter(Field::isNonScalar).map(Field::toTypeSpec)
    return addTypes(typeSpecs)
  }

  private fun TypeSpec.Builder.addInlineFragmentTypes(inlineFragments: List<InlineFragment>): TypeSpec.Builder {
    val typeSpecs = inlineFragments.map { it.toTypeSpec() }
    return addTypes(typeSpecs)
  }

  private fun TypeSpec.Builder.addInlineFragmentAccessorMethods(inlineFragments: List<InlineFragment>): TypeSpec.Builder {
    val methodSpecs = inlineFragments.map {
      MethodSpec.methodBuilder(it.interfaceName().decapitalize())
          .returns(ClassName.get("", it.interfaceName()).annotated(JavaPoetUtils.NULLABLE_ANNOTATION))
          .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
          .build()
    }
    return addMethods(methodSpecs)
  }

  companion object {
    private val FRAGMENTS_INTERFACE_NAME = "Fragments"

    private fun newFragmentAccessorMethodSpec(): MethodSpec =
        MethodSpec.methodBuilder("fragments")
            .returns(ClassName.get("", FRAGMENTS_INTERFACE_NAME))
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .build()

    /** Returns a generic `Fragments` interface with methods for each of the provided fragments */
    private fun newFragmentInterfaceSpec(fragments: List<String>): TypeSpec =
        TypeSpec.interfaceBuilder(FRAGMENTS_INTERFACE_NAME)
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addMethods(fragments.map {
              MethodSpec.methodBuilder(it.decapitalize())
                  .returns(ClassName.get("", it.capitalize()))
                  .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                  .build()
            })
            .build()
  }
}