package com.apollographql.apollo.debugserver.internal.graphql

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.CacheDumpProviderContext
import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.ast.GQLValue
import com.apollographql.execution.Coercing
import com.apollographql.execution.ExecutableSchema
import com.apollographql.execution.ExternalValue
import com.apollographql.execution.StringCoercing
import com.apollographql.execution.annotation.GraphQLName
import com.apollographql.execution.annotation.GraphQLQuery
import com.apollographql.execution.annotation.GraphQLScalar
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
// Prefixing private fields with _ to work around https://github.com/google/ksp/issues/2135
internal class Query(private val _apolloClients: AtomicReference<Map<ApolloClient, String>>) {
  private fun graphQLApolloClients() =
    _apolloClients.get().map { (apolloClient, apolloClientId) ->
      GraphQLApolloClient(
          _id = apolloClientId,
          _apolloClient = apolloClient
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
    private val _id: String,
    private val _apolloClient: ApolloClient,
) {
  fun id(): ID = _id

  fun displayName() = _id

  fun normalizedCaches(): List<NormalizedCache> {
    val cacheDumpProvider = _apolloClient.executionContext[CacheDumpProviderContext]?.cacheDumpProvider ?: return emptyList()
    return cacheDumpProvider().map { (displayName, cacheDump) ->
      NormalizedCache(apolloClientId = _id, _displayName = displayName, _cacheDump = cacheDump)
    }
  }

  fun normalizedCache(id: ID): NormalizedCache? {
    return normalizedCaches().firstOrNull { it.id() == id }
  }
}

internal class NormalizedCache(
    apolloClientId: ID,
    private val _displayName: String,
    private val _cacheDump: CacheDump,
) {
  private val id: String = "$apolloClientId:$_displayName"
  fun id(): ID = id

  fun displayName() = _displayName

  fun recordCount() = _cacheDump.count()

  fun records(): List<GraphQLRecord> =
    _cacheDump.map { (key, record) -> GraphQLRecord(_key = key, _sizeInBytes = record.first, _fields = record.second) }
}

@GraphQLScalar(FieldsCoercing::class)
typealias Fields = Map<String, Any?>

@GraphQLName("Record")
internal class GraphQLRecord(
    private val _key: String,
    private val _sizeInBytes: Int,
    private val _fields: Fields,
) {
  fun key(): String = _key

  fun fields(): Fields = _fields

  fun sizeInBytes(): Int = _sizeInBytes
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
