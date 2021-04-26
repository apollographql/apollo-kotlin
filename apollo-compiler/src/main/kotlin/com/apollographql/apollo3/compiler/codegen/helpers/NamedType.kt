package com.apollographql.apollo3.compiler.codegen.helpers

import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.codegen.CgContext
import com.apollographql.apollo3.compiler.unified.ir.IrInputField
import com.apollographql.apollo3.compiler.unified.ir.IrOptionalType
import com.apollographql.apollo3.compiler.unified.ir.IrType
import com.apollographql.apollo3.compiler.unified.ir.IrVariable
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.asClassName

class NamedType(
    val graphQlName: String,
    val description: String?,
    val deprecationReason: String?,
    val type: IrType,
)


internal fun NamedType.toParameterSpec(context: CgContext): ParameterSpec {
  return ParameterSpec
      .builder(
          // we use property for parameters as these are ultimately data classes
          name = context.layout.propertyName(graphQlName),
          type = context.resolver.resolveType(type)
      )
      .applyIf(description?.isNotBlank() == true) { addKdoc("%L\n", description!!) }
      .applyIf(type is IrOptionalType) { defaultValue("%T", Optional.Absent::class.asClassName()) }
      .build()
}


fun IrInputField.toNamedType() = NamedType(
    graphQlName = name,
    type = type,
    description = description,
    deprecationReason = deprecationReason,
)

fun IrVariable.toNamedType() = NamedType(
    graphQlName = name,
    type = type,
    description = null,
    deprecationReason = null,
)
