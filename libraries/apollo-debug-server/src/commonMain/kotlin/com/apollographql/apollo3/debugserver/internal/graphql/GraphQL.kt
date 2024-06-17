package com.apollographql.apollo3.debugserver.internal.graphql

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.ast.GQLValue
import com.apollographql.apollo3.cache.normalized.api.CacheKey
import com.apollographql.apollo3.cache.normalized.api.Record
import com.apollographql.apollo3.cache.normalized.apolloStore
import com.apollographql.execution.Coercing
import com.apollographql.execution.ExecutableSchema
import com.apollographql.execution.StringCoercing
import com.apollographql.execution.annotation.GraphQLCoercing
import com.apollographql.execution.annotation.GraphQLName
import com.apollographql.execution.annotation.GraphQLQueryRoot
import com.apollographql.execution.annotation.GraphQLScalar
import com.apollographql.execution.internal.ExternalValue
import com.apollographql.execution.internal.InternalValue
import com.apollographql.execution.parsePostGraphQLRequest
import okio.Buffer
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KClass

@GraphQLScalar
internal typealias ID = String

@GraphQLCoercing
internal object IDCoercing: Coercing<ID> by StringCoercing

internal class GraphQL(
    private val apolloClients: AtomicReference<Map<ApolloClient, String>>,
) {
  private val executableSchema: ExecutableSchema by lazy {
    ApolloDebugServerExecutableSchemaBuilder()
        .queryRoot {
          Query(apolloClients)
        }.build()
  }

  fun executeGraphQL(jsonBody: String): String {
    val graphQLRequestResult = Buffer().writeUtf8(jsonBody).parsePostGraphQLRequest()
    if (!graphQLRequestResult.isSuccess) {
      return graphQLRequestResult.exceptionOrNull()!!.message!!
    }

    val graphQlResponse = executableSchema.execute(graphQLRequestResult.getOrThrow(), ExecutionContext.Empty)

    val buffer = Buffer()
    graphQlResponse.serialize(buffer)
    return buffer.readUtf8()
  }
}

/**
 * The root query
 */
@GraphQLQueryRoot
internal class Query(private val apolloClients: AtomicReference<Map<ApolloClient, String>>) {
  private fun graphQLApolloClients() =
    apolloClients.get().map { (apolloClient, apolloClientId) ->
      GraphQLApolloClient(
          id = apolloClientId,
          apolloClient = apolloClient
      )
    }

  fun apolloClients(): List<GraphQLApolloClient> {
    return graphQLApolloClients()
  }

  /**
   * Returns null if an ApolloClient with the given id is not found.
   */
  fun apolloClient(id: ID): GraphQLApolloClient? {
    return graphQLApolloClients().firstOrNull { it.id() == id }
  }
}

@GraphQLName("ApolloClient")
internal class GraphQLApolloClient(
    private val id: String,
    private val apolloClient: ApolloClient,
) {
  fun id(): ID = id

  fun displayName() = id

  fun normalizedCaches(): List<NormalizedCache> {
    val apolloStore = runCatching { apolloClient.apolloStore }.getOrNull() ?: return emptyList()
    return apolloStore.dump().map {
      NormalizedCache(id, it.key, it.value)
    }
  }

  fun normalizedCache(id: ID): NormalizedCache? {
    return normalizedCaches().firstOrNull { it.id() == id }
  }
}

internal class NormalizedCache(
    apolloClientId: ID,
    private val clazz: KClass<*>,
    private val records: Map<String, Record>,
) {
  private val id: String = "$apolloClientId:${clazz.normalizedCacheName()}"
  fun id(): ID = id

  fun displayName() = clazz.normalizedCacheName()

  fun recordCount() = records.count()

  fun records(): List<GraphQLRecord> = records.map { GraphQLRecord(it.value) }
}

@GraphQLScalar
typealias Fields = Map<String, Any?>

@GraphQLName("Record")
internal class GraphQLRecord(
    private val record: Record,
) {
  fun key(): String = record.key

  fun fields(): Fields = record.fields

  fun sizeInBytes(): Int = record.sizeInBytes
}

@GraphQLCoercing
internal class FieldsCoercing : Coercing<Fields> {
  // Taken from JsonRecordSerializer
  @Suppress("UNCHECKED_CAST")
  private fun InternalValue.toExternal(): ExternalValue {
    return when (this) {
      null -> this
      is String -> this
      is Boolean -> this
      is Int -> this
      is Long -> this
      is Double -> this
      is CacheKey -> this.serialize()
      is List<*> -> {
        map { it.toExternal() }
      }

      is Map<*, *> -> {
        mapValues { it.value.toExternal() }
      }

      else -> error("Unsupported record value type: '$this'")
    }
  }

  override fun serialize(internalValue: Map<String, Any?>): ExternalValue {
    return internalValue.toExternal()
  }

  override fun deserialize(value: ExternalValue): Map<String, Any?> {
    TODO("Not yet implemented")
  }

  override fun parseLiteral(gqlValue: GQLValue): Map<String, Any?> {
    TODO("Not yet implemented")
  }
}

private fun KClass<*>.normalizedCacheName(): String = qualifiedName ?: toString()
