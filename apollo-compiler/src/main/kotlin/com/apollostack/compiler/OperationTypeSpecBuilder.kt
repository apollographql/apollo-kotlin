package com.apollostack.compiler

import com.apollostack.compiler.ir.CodeGenerator
import com.apollostack.compiler.ir.Field
import com.apollostack.compiler.ir.Fragment
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

class OperationTypeSpecBuilder(
    val operationName: String,
    val fields: List<Field>,
    val allFragments: List<Fragment>) : CodeGenerator {

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
            TypeSpec.interfaceBuilder(field.normalizedName())
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addMethod(Fragment.genericAccessorMethodSpec())
                .addType(Fragment.genericInterfaceSpec(fragments))
                .build()
          }

  /** Returns a list of types referenced by the inner fields in the provided fields */
  private fun innerTypes(fields: List<Field>): List<TypeSpec> =
      fields.filter(Field::isNonScalar)
          .map { buildTypeSpecs(it.normalizedName(), it.fields!!, referencedFragments(it)) }

  /** Filters all fragments to only the ones referenced by the provided field */
  private fun referencedFragments(field: Field) =
      allFragments.filter { field.fragmentSpreads?.contains(it.fragmentName) ?: false }
}