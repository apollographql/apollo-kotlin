package com.apollostack.compiler

import com.apollostack.compiler.ir.CodeGenerator
import com.apollostack.compiler.ir.Field
import com.apollostack.compiler.ir.Fragment
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

/**
 * TODO: Move this code to the class Operation so it's consistent with other classes implementing
 * the CodeGenerator interface.
 */
class OperationTypeSpecBuilder(
    val operationName: String,
    val fields: List<Field>,
    val allFragments: List<Fragment>
) : CodeGenerator {
  override fun toTypeSpec(): TypeSpec = buildTypeSpecs(operationName, fields, allFragments)

  private fun buildTypeSpecs(typeName: String, fields: List<Field>, fragments: List<Fragment>) =
      TypeSpec.interfaceBuilder(typeName)
          .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
          .addMethods(fields.map(Field::toMethodSpec))
          .addTypes(innerTypes(fields))
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
                .addMethod(fragmentAccessorMethodSpec())
                .addType(fragmentInterfaceSpec(fragments))
                .build()
          }

  /** Returns a list of types referenced by the inner fields in the provided fields */
  private fun innerTypes(fields: List<Field>): List<TypeSpec> =
      fields.filter(Field::isNonScalar)
          .map { buildTypeSpecs(it.normalizedName(), it.fields!!, referencedFragments(it)) }

  /** Filters all fragments to only the ones referenced by the provided field */
  private fun referencedFragments(field: Field) =
      allFragments.filter { field.fragmentSpreads?.contains(it.fragmentName) ?: false }

  companion object {
    private val INTERFACE_NAME = "Fragments"

    fun fragmentAccessorMethodSpec(): MethodSpec =
        MethodSpec.methodBuilder("fragments")
            .returns(ClassName.get("", INTERFACE_NAME))
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .build()

    /** Returns a generic `Fragments` interface with methods for each of the provided fragments */
    fun fragmentInterfaceSpec(fragments: List<Fragment>): TypeSpec =
        TypeSpec.interfaceBuilder(INTERFACE_NAME)
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addMethods(fragments.map(Fragment::toMethodSpec))
            .build()
  }
}