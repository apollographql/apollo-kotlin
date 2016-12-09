package com.apollostack.compiler

import com.apollostack.compiler.ir.CodeGenerator
import com.apollostack.compiler.ir.Field
import com.apollostack.compiler.ir.Fragment
import com.squareup.javapoet.TypeSpec

/**
 * TODO: Move this code to the class Operation so it's consistent with other classes implementing
 * the CodeGenerator interface.
 */
class OperationTypeSpecBuilder(
    val operationName: String,
    val fields: List<Field>
) : CodeGenerator {

  override fun toTypeSpec(fragments: List<Fragment>): TypeSpec =
    // TODO: This is a bit weird that we can' call field.toTypeSpec directly, also we need to be
    // able to inject the parameters directly (including the list of fragments), so we use
    // FieldTypeSpecBuilder directly.
    FieldTypeSpecBuilder().build(operationName, fields, fragments, emptyList())
        .resolveNestedTypeNameDuplication(emptyList())
}