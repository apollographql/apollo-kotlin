package com.apollographql.apollo3.compiler.unified.codegen

internal fun List<CgVariable>.toCgAdapter(name: String): CgAdapter {
  return CgMonomorphicAdapter(
      name = name,
      adaptedFields = emptyList()
  )
}