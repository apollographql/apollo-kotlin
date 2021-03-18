package com.apollographql.apollo3.compiler.unified.codegen

internal fun List<CGVariable>.toCGAdapter(name: String): CGAdapter {
  return CGMonomorphicAdapter(
      name = name,
      adaptedFields = emptyList()
  )
}