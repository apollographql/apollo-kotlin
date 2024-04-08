package com.apollographql.apollo3.cache.normalized.api

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.CompiledNamedType
import com.apollographql.apollo3.api.InterfaceType
import com.apollographql.apollo3.api.ObjectType

@ApolloExperimental
interface EmbeddedFieldsProvider {
  fun getEmbeddedFields(context: EmbeddedFieldsContext): List<String>
}

@ApolloExperimental
class EmbeddedFieldsContext(
    val parentType: CompiledNamedType,
)

@ApolloExperimental
object DefaultEmbeddedFieldsProvider : EmbeddedFieldsProvider {
  override fun getEmbeddedFields(context: EmbeddedFieldsContext): List<String> {
    return context.parentType.embeddedFields
  }
}

private val CompiledNamedType.embeddedFields: List<String>
  get() = when (this) {
    is ObjectType -> embeddedFields
    is InterfaceType -> embeddedFields
    else -> emptyList()
  }

@ApolloExperimental
class ConnectionEmbeddedFieldsProvider(
    connectionFields: Map<String, List<String>>,
    connectionTypes: Set<String>,
) : EmbeddedFieldsProvider {
  companion object {
    private val connectionFieldsToEmbed = listOf("pageInfo", "edges")
  }

  private val embeddedFields = connectionFields + connectionTypes.associateWith { connectionFieldsToEmbed }

  override fun getEmbeddedFields(context: EmbeddedFieldsContext): List<String> {
    return embeddedFields[context.parentType.name].orEmpty()
  }
}
