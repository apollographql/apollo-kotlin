package com.apollographql.apollo3.compiler.unified.codegen.helpers

import com.apollographql.apollo3.compiler.unified.IrAnyType
import com.apollographql.apollo3.compiler.unified.IrBooleanType
import com.apollographql.apollo3.compiler.unified.IrCompoundType
import com.apollographql.apollo3.compiler.unified.IrCustomScalarType
import com.apollographql.apollo3.compiler.unified.IrEnumType
import com.apollographql.apollo3.compiler.unified.IrField
import com.apollographql.apollo3.compiler.unified.IrFieldSet
import com.apollographql.apollo3.compiler.unified.IrFloatType
import com.apollographql.apollo3.compiler.unified.IrIdType
import com.apollographql.apollo3.compiler.unified.IrInputObjectType
import com.apollographql.apollo3.compiler.unified.IrIntType
import com.apollographql.apollo3.compiler.unified.IrListType
import com.apollographql.apollo3.compiler.unified.IrNonNullType
import com.apollographql.apollo3.compiler.unified.IrStringType
import com.apollographql.apollo3.compiler.unified.IrType
import com.apollographql.apollo3.compiler.unified.ModelPath
import com.apollographql.apollo3.compiler.unified.codegen.kotlinTypeName
import com.apollographql.apollo3.compiler.unified.codegen.typeName
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName

fun IrType.typeName(fieldSet: IrFieldSet? = null): TypeName {
  if (this is IrNonNullType) {
    return ofType.typeName(fieldSet).copy(nullable = false)
  }

  return when (this) {
    is IrNonNullType -> error("") // make the compiler happy, this case is handled as a fast path
    is IrListType -> List::class.asClassName().parameterizedBy(ofType.typeName(fieldSet))
    is IrStringType -> String::class.asTypeName()
    is IrFloatType -> Double::class.asTypeName()
    is IrIntType -> Int::class.asTypeName()
    is IrBooleanType -> Boolean::class.asTypeName()
    is IrIdType -> String::class.asTypeName()
    is IrAnyType -> Any::class.asTypeName()
    is IrCustomScalarType -> customScalar.kotlinTypeName()
    is IrEnumType -> enum.typeName()
    is IrInputObjectType -> inputObject().typeName()
    is IrCompoundType -> fieldSet?.typeName() ?: error("IrField.typeName() instead")
  }.copy(nullable = true)
}

fun IrField.typeName(): TypeName {
  return type.typeName(typeFieldSet)
}

fun IrFieldSet.typeName(): TypeName {
  return fullPath.typeName()
}

fun ModelPath.typeName(): ClassName {
  return ClassName(
      packageName = packageName,
      simpleNames = elements
  )
}
