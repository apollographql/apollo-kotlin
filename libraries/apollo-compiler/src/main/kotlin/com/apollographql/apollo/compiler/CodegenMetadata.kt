package com.apollographql.apollo.compiler

import com.apollographql.apollo.annotations.ApolloInternal
import com.apollographql.apollo.compiler.codegen.ResolverClassName
import com.apollographql.apollo.compiler.codegen.ResolverEntry
import com.apollographql.apollo.compiler.codegen.ResolverKeyKind
import kotlinx.serialization.Serializable

/**
 * [CodegenMetadata] contains information about what target classes were generated for each GraphQL types
 * so that downstream modules can reuse them.
 */
@Serializable
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
