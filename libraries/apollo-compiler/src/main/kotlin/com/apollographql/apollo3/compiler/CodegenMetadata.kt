package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.compiler.codegen.ResolverClassName
import com.apollographql.apollo3.compiler.codegen.ResolverEntry
import com.apollographql.apollo3.compiler.codegen.ResolverKeyKind
import kotlinx.serialization.Serializable

/**
 * resolver info used by the codegen to lookup already existing ClassNames
 */
@Serializable
@ApolloInternal
class CodegenMetadata(
    val targetLanguage: TargetLanguage,
    val entries: List<ResolverEntry>

) {
  operator fun plus(other: CodegenMetadata): CodegenMetadata {
    if (entries.isEmpty()) {
      return other
    } else if (other.entries.isEmpty()) {
      return this
    }

    check(targetLanguage == other.targetLanguage) {
      "Apollo: incompatible metadata: '${other.targetLanguage}' != '${targetLanguage}'"
    }
    return CodegenMetadata(
        targetLanguage = targetLanguage,
        entries = entries + other.entries
    )
  }
}

@ApolloInternal
fun CodegenMetadata.resolveSchemaType(name: String): ResolverClassName? {
  return entries.firstOrNull { it.key.kind == ResolverKeyKind.SchemaType && it.key.id == name }?.className
}
