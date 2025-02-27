package com.apollographql.apollo.compiler

import com.apollographql.apollo.compiler.codegen.ResolverClassName
import com.apollographql.apollo.compiler.codegen.ResolverEntry
import com.apollographql.apollo.compiler.codegen.ResolverKeyKind
import kotlinx.serialization.Serializable

internal const val CODEGEN_METADATA_VERSION = "0.0.0"

/**
 * [CodegenMetadata] contains information about what target classes were generated for each GraphQL types
 * so that downstream modules can reuse them.
 */
@Serializable
class CodegenMetadata(
    val version: String,
    val targetLanguage: TargetLanguage,
    val entries: List<ResolverEntry>,
    val inlineProperties: Map<String, String?>,
    val scalarTargets: Map<String, String>,
    val scalarAdapters: Map<String, String>,
    val scalarIsUserDefined: Map<String, Boolean>
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
        entries = entries + other.entries,
        version = CODEGEN_METADATA_VERSION,
        inlineProperties = inlineProperties + other.inlineProperties,
        scalarTargets = scalarTargets + other.scalarTargets,
        scalarAdapters = scalarAdapters + other.scalarAdapters,
        scalarIsUserDefined = scalarIsUserDefined + other.scalarIsUserDefined
    )
  }
}

internal fun CodegenMetadata.resolveSchemaType(name: String): ResolverClassName? {
  return entries.firstOrNull { it.key.kind == ResolverKeyKind.SchemaType && it.key.id == name }?.className
}
