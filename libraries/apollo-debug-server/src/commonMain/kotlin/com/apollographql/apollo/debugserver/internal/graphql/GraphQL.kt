package com.apollographql.apollo.debugserver.internal.graphql

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.CacheDumpProviderContext
import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.ast.GQLValue
import com.apollographql.execution.Coercing
import com.apollographql.execution.ExecutableSchema
import com.apollographql.execution.StringCoercing
import com.apollographql.execution.annotation.GraphQLName
import com.apollographql.execution.annotation.GraphQLQuery
import com.apollographql.execution.annotation.GraphQLScalar
import com.apollographql.execution.ExternalValue
import com.apollographql.execution.parseAsGraphQLRequest
import kotlinx.coroutines.runBlocking
import okio.Buffer
import java.util.concurrent.atomic.AtomicReference

@GraphQLScalar(StringCoercing::class)
internal typealias ID = String

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
    val graphQLRequestResult = Buffer().writeUtf8(jsonBody).parseAsGraphQLRequest()
    if (!graphQLRequestResult.isSuccess) {
      return graphQLRequestResult.exceptionOrNull()!!.message!!
    }

    val graphQlResponse = runBlocking {
      executableSchema.execute(graphQLRequestResult.getOrThrow(), ExecutionContext.Empty)
    }

    val buffer = Buffer()
    graphQlResponse.serialize(buffer)
    return buffer.readUtf8()
  }
}

/**
 * The root query
 */
@GraphQLQuery
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
    val cacheDumpProvider = apolloClient.executionContext[CacheDumpProviderContext]?.cacheDumpProvider ?: return emptyList()
    return cacheDumpProvider().map { (displayName, cacheDump) ->
      NormalizedCache(apolloClientId = id, displayName = displayName, cacheDump = cacheDump)
    }
  }

  fun normalizedCache(id: ID): NormalizedCache? {
    return normalizedCaches().firstOrNull { it.id() == id }
  }
}

internal class NormalizedCache(
    apolloClientId: ID,
    private val displayName: String,
    private val cacheDump: CacheDump,
) {
  private val id: String = "$apolloClientId:$displayName"
  fun id(): ID = id

  fun displayName() = displayName

  fun recordCount() = cacheDump.count()

  fun records(): List<GraphQLRecord> =
    cacheDump.map { (key, record) -> GraphQLRecord(key = key, sizeInBytes = record.first, fields = record.second) }
}

@GraphQLScalar(FieldsCoercing::class)
typealias Fields = Map<String, Any?>

@GraphQLName("Record")
internal class GraphQLRecord(
    private val key: String,
    private val sizeInBytes: Int,
    private val fields: Fields,
) {
  fun key(): String = key

  fun fields(): Fields = fields

  fun sizeInBytes(): Int = sizeInBytes
}

internal object FieldsCoercing : Coercing<Fields> {
  override fun serialize(internalValue: Map<String, Any?>): ExternalValue {
    return internalValue
  }

  override fun deserialize(value: ExternalValue): Map<String, Any?> {
    throw NotImplementedError()
  }

  override fun parseLiteral(value: GQLValue): Map<String, Any?> {
    throw NotImplementedError()
  }
}

private typealias CacheDump = Map<
    // Record key
    String,

    // Record size and fields
    Pair<
        // Record size in bytes
        Int,

        // Record fields
        Map<String, Any?>,
        >,
    >

private typealias CacheDumpProvider = () -> Map<
    // Cache name
    String,

    // Cache dump
    CacheDump,
    >
