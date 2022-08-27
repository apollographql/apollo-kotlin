package com.apollographql.apollo3.compiler.codegen.java.helpers

import com.apollographql.apollo3.compiler.ir.IrScalarType
import com.apollographql.apollo3.compiler.ir.IrType

val BuiltInScalars = setOf("Float", "Double", "Int", "String", "Boolean")

internal fun IrType.isCustomScalar(): Boolean =
  leafType() is IrScalarType && !BuiltInScalars.contains((leafType() as IrScalarType).name)
