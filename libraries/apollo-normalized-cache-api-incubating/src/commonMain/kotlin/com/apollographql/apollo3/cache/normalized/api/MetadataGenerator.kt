package com.apollographql.apollo3.cache.normalized.api

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.CompiledField
import com.apollographql.apollo3.api.Executable
import com.apollographql.apollo3.api.json.ApolloJsonElement

/**
 * A generator for arbitrary metadata associated with objects.
 * For example, information about pagination can later be used to merge pages (see [RecordMerger]).
 *
 * The metadata is stored and attached to the object's field in the [Record] resulting from the normalization.
 * For instance, given the query `query MyQuery { foo }` and an implementation of [metadataForObject] returning
 * `mapOf("key", 0)`, the resulting Record will look like `fields: { foo: bar }, metadata: { foo: { key: 0 } }`.
 *
 * @see [Record.metadata]
 */
@ApolloExperimental
interface MetadataGenerator {
  /**
   * Returns metadata for the given object.
   * This is called for every field in the response, during normalization.
   *
   * The type of the object can be found in `context.field.type`.
   *
   * @param obj the object to generate metadata for.
   * @param context contains the object's field and the variables of the operation execution.
   */
  fun metadataForObject(obj: ApolloJsonElement, context: MetadataGeneratorContext): Map<String, ApolloJsonElement>
}

/**
 * Additional context passed to the [MetadataGenerator.metadataForObject] method.
 */
@ApolloExperimental
class MetadataGeneratorContext(
    val field: CompiledField,
    val variables: Executable.Variables,
) {
  fun argumentValue(argumentName: String): ApolloJsonElement {
    return field.argumentValue(argumentName, variables).getOrNull()
  }

  fun allArgumentValues(): Map<String, ApolloJsonElement> {
    return field.argumentValues(variables) { !it.definition.isPagination }
  }
}

/**
 * Default [MetadataGenerator] that returns empty metadata.
 */
@ApolloExperimental
object EmptyMetadataGenerator : MetadataGenerator {
  override fun metadataForObject(obj: ApolloJsonElement, context: MetadataGeneratorContext): Map<String, ApolloJsonElement> = emptyMap()
}

/**
 * A [MetadataGenerator] that generates metadata for
 * [Relay connection types](https://relay.dev/graphql/connections.htm#sec-Connection-Types).
 * Collaborates with [ConnectionRecordMerger] to merge pages of a connection.
 *
 * Either `pageInfo.startCursor` and `pageInfo.endCursor`, or `edges.cursor` must be present in the selection.
 */
@ApolloExperimental
class ConnectionMetadataGenerator(private val connectionTypes: Set<String>) : MetadataGenerator {
  @Suppress("UNCHECKED_CAST")
  override fun metadataForObject(obj: ApolloJsonElement, context: MetadataGeneratorContext): Map<String, ApolloJsonElement> {
    if (context.field.type.rawType().name in connectionTypes) {
      obj as Map<String, ApolloJsonElement>
      val pageInfo = obj["pageInfo"] as? Map<String, ApolloJsonElement>
      val edges = obj["edges"] as? List<Map<String, ApolloJsonElement>>
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
