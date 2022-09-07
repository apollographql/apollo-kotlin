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
    return field.resolveArgument(argumentName, variables)
  }

  fun allArgumentValues(): Map<String, Any?> {
    return field.arguments.associate { it.name to argumentValue(it.name) }
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
      val edges = obj["edges"] as List<Map<String, Any?>>
      val startCursor = edges.firstOrNull()?.get("cursor") as String?
      val endCursor = edges.lastOrNull()?.get("cursor") as String?
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
