package com.apollographql.apollo3.compiler.unified.codegen.helpers

import com.apollographql.apollo3.compiler.backend.codegen.adapterPackageName
import com.apollographql.apollo3.compiler.backend.codegen.kotlinNameForResponseAdapter
import com.apollographql.apollo3.compiler.unified.IrFieldSet
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName

fun IrFieldSet.adapterTypeName(): TypeName {
  // Go from:
  // [TestQuery, Data, Hero, ...]
  // To:
  // [TestQuery_ResponseAdapter, Data, Hero, ...]
  return ClassName(
      packageName = adapterPackageName(fullPath.packageName),
      listOf(kotlinNameForResponseAdapter(fullPath.elements.first())) + fullPath.elements.drop(1)
  )
}