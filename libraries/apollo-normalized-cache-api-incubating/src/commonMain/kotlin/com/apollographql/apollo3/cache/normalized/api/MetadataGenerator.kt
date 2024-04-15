package com.apollographql.apollo3.cache.normalized.api

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.CompiledField
import com.apollographql.apollo3.api.Executable

@ApolloExperimental
interface MetadataGenerator {
  fun metadataForObject(obj: Any?, context: MetadataGeneratorContext): Map<String, Any?>
}

@ApolloExperimental
class MetadataGeneratorContext(
    val field: CompiledField,
    val variables: Executable.Variables,
) {
  fun argumentValue(argumentName: String): Any? {
    return field.argumentValue(argumentName, variables).getOrNull()
  }

  fun allArgumentValues(): Map<String, Any?> {
    return field.argumentValues(variables) { !it.definition.isPagination }
  }
}

@ApolloExperimental
object EmptyMetadataGenerator : MetadataGenerator {
  override fun metadataForObject(obj: Any?, context: MetadataGeneratorContext): Map<String, Any?> = emptyMap()
}

@ApolloExperimental
class ConnectionMetadataGenerator(private val connectionTypes: Set<String>) : MetadataGenerator {
  @Suppress("UNCHECKED_CAST")
  override fun metadataForObject(obj: Any?, context: MetadataGeneratorContext): Map<String, Any?> {
    if (context.field.type.rawType().name in connectionTypes) {
      obj as Map<String, Any?>
      val pageInfo = obj["pageInfo"] as? Map<String, Any?>
      val edges = obj["edges"] as? List<Map<String, Any?>>
      if (edges == null && pageInfo == null) {
        return emptyMap()
      }
      // Get start and end cursors from the PageInfo, if present in the selection. Else, get it from the first and last edges.
      val startCursor = pageInfo?.get("startCursor") as String? ?: edges?.firstOrNull()?.get("cursor") as String?
      val endCursor = pageInfo?.get("endCursor") as String? ?: edges?.lastOrNull()?.get("cursor") as String?
      return mapOf(
          "startCursor" to startCursor,
          "endCursor" to endCursor,
          "before" to context.argumentValue("before"),
          "after" to context.argumentValue("after"),
      )
    }
    return emptyMap()
  }
}
