package com.apollographql.apollo3.compiler.unified.codegen

import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.unified.IrBooleanType
import com.apollographql.apollo3.compiler.unified.IrCustomScalarType
import com.apollographql.apollo3.compiler.unified.IrEnumType
import com.apollographql.apollo3.compiler.unified.IrField
import com.apollographql.apollo3.compiler.unified.IrFieldSet
import com.apollographql.apollo3.compiler.unified.IrFloatType
import com.apollographql.apollo3.compiler.unified.IrIdType
import com.apollographql.apollo3.compiler.unified.IrInputObjectType
import com.apollographql.apollo3.compiler.unified.IrIntType
import com.apollographql.apollo3.compiler.unified.IrInterfaceType
import com.apollographql.apollo3.compiler.unified.IrListType
import com.apollographql.apollo3.compiler.unified.IrNonNullType
import com.apollographql.apollo3.compiler.unified.IrObjectType
import com.apollographql.apollo3.compiler.unified.IrStringType
import com.apollographql.apollo3.compiler.unified.IrType
import com.apollographql.apollo3.compiler.unified.IrUnionType
import com.apollographql.apollo3.compiler.unified.ModelPath
import com.apollographql.apollo3.compiler.unified.PathElement
import com.apollographql.apollo3.compiler.unified.TypeSet
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName

private fun modelName(typeSet: TypeSet, responseName: String): String {
  return (typeSet.sorted() + responseName).map { it.capitalize() }.joinToString("")
}

private fun IrFieldSet.toTypeName(): TypeName {
  return fullPath.toTypeName()
}

private fun ModelPath.toTypeName() = ClassName(
    packageName = "com.example",
    elements.map { modelName(it.typeSet - it.fieldType, it.responseName) }
)

fun IrType.toVariableTypeName(): TypeName {
  return toTypeName(null)
}

fun IrType.toInputTypeName(): TypeName {
  return toTypeName(null)
}

/**
 * If you expect to have a non-scalar output type somewhere, you **must** pass it in [compoundType]
 */
fun IrType.toTypeName(compoundTypeName: TypeName?): TypeName {
  if (this is IrNonNullType) {
    return ofType.toTypeName(compoundTypeName).copy(nullable = false)
  }

  return when (this) {
    is IrNonNullType -> error("") // make the compiler happy, this case is handled as a fast path
    is IrListType -> List::class.asClassName().parameterizedBy(ofType.toTypeName(compoundTypeName))
    is IrStringType -> String::class.asTypeName()
    is IrFloatType -> Double::class.asTypeName()
    is IrIntType -> Int::class.asTypeName()
    is IrBooleanType -> Boolean::class.asTypeName()
    is IrIdType -> String::class.asTypeName()
    is IrCustomScalarType -> ClassName(
        packageName = "com.example.type",
        "Scalars",
        name
    )
    is IrEnumType -> ClassName(
        packageName = "com.example.type",
        name
    )
    is IrInputObjectType -> ClassName(
        packageName = "com.example.type",
        name
    )
    is IrObjectType,
    is IrInterfaceType,
    is IrUnionType,
    -> compoundTypeName ?: error("compoundType is required to build this CgType")
  }.copy(nullable = true)
}

private fun IrField.typeName(): TypeName {
  return type.toTypeName(baseFieldSet?.toTypeName())
}

fun IrFieldSet.toTypeSpec(): TypeSpec {
  return TypeSpec.interfaceBuilder(modelName(typeSet - fieldType, responseName))
      .addProperties(
          fields.map {
            PropertySpec.builder(
                it.responseName,
                it.typeName()
            ).applyIf(it.override) {
              addModifiers(KModifier.OVERRIDE)
            }.build()
          }
      )
      .addTypes(
          fields.flatMap {
            it.fieldSets.map { it.toTypeSpec() }
          }
      )
      .addSuperinterfaces(
          implements.map {
            it.toTypeName()
          }
      )
      .build()
}