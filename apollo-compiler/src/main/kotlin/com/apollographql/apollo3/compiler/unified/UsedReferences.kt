package com.apollographql.apollo3.compiler.unified

import com.apollographql.apollo3.compiler.frontend.Schema

class UsedReferences(
    val enums: Set<String>,
    val inputObjects: Set<String>,
    val customScalars: Set<String>,
    val namedFragments: Set<String>
) {
  operator fun plus(other: UsedReferences): UsedReferences {
    return UsedReferences(
        enums = enums + other.enums,
        inputObjects = inputObjects + other.inputObjects,
        customScalars = customScalars + other.customScalars,
        namedFragments = namedFragments + other.namedFragments,
    )
  }

  companion object {
    val Empty = UsedReferences(emptySet(), emptySet(), emptySet(), emptySet())
  }
}
