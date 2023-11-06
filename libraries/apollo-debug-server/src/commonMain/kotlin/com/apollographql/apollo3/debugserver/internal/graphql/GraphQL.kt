package com.apollographql.apollo3.debugserver.internal.graphql

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.annotations.ApolloAdapter
import com.apollographql.apollo3.annotations.ApolloObject
import com.apollographql.apollo3.annotations.GraphQLName
import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.api.json.writeAny
import com.apollographql.apollo3.ast.toGQLDocument
import com.apollographql.apollo3.ast.toSchema
import com.apollographql.apollo3.cache.normalized.api.Record
import com.apollographql.apollo3.cache.normalized.apolloStore
import com.apollographql.apollo3.debugserver.internal.graphql.execution.ApolloDebugServerExecutableSchemaBuilder
import com.apollographql.apollo3.execution.ExecutableSchema
import com.apollographql.apollo3.execution.GraphQLRequest
import com.apollographql.apollo3.execution.GraphQLRequestError
import com.apollographql.apollo3.execution.parsePostGraphQLRequest
import kotlinx.coroutines.runBlocking
import okio.Buffer
import kotlin.reflect.KClass

internal expect fun getExecutableSchema(): String

internal class GraphQL(
    private val apolloClients: Map<ApolloClient, String>,
) {
  private val executableSchema: ExecutableSchema by lazy {
    val schema = getExecutableSchema()
        .toGQLDocument()
        .toSchema()

    ApolloDebugServerExecutableSchemaBuilder(schema) {
      Query(apolloClients)
    }.build()
  }

  fun executeGraphQL(jsonBody: String): String {
    val graphQLRequestResult = Buffer().writeUtf8(jsonBody).parsePostGraphQLRequest()
    if (graphQLRequestResult is GraphQLRequestError) {
      return graphQLRequestResult.message
    }
    graphQLRequestResult as GraphQLRequest

    val graphQlResponse = executableSchema.execute(graphQLRequestResult, ExecutionContext.Empty)

    val buffer = Buffer()
    graphQlResponse.serialize(buffer)
    return buffer.readUtf8()
  }
}

internal class ApolloDebugContext(
    val apolloClients: Map<ApolloClient, String>,
    val dumps: Map<ApolloClient, Map<KClass<*>, Map<String, Record>>>,
)

@ApolloObject
internal class Query(private val apolloClients: Map<ApolloClient, String>) {
  private fun graphQLApolloClients() =
      apolloClients.map { (apolloClient, apolloClientId) ->
        GraphQLApolloClient(
            id = apolloClientId,
            apolloClient = apolloClient
        )
      }

  fun apolloClients(): List<GraphQLApolloClient> {
    return graphQLApolloClients()
  }

  fun apolloClient(id: String): GraphQLApolloClient? {
    return graphQLApolloClients().firstOrNull { it.id() == id }
  }
}

@ApolloObject
@GraphQLName("ApolloClient")
internal class GraphQLApolloClient(
    private val id: String,
    private val apolloClient: ApolloClient,
) {
  fun id() = id

  fun displayName() = id

  fun normalizedCaches(): List<NormalizedCache> = runBlocking { apolloClient.apolloStore.dump() }.map {
    NormalizedCache(id, it.key, it.value)
  }

  fun normalizedCache(id: String): NormalizedCache? {
    return normalizedCaches().firstOrNull { it.id() == id }
  }
}

@ApolloObject
internal class NormalizedCache(
    private val apolloClientId: String,
    private val clazz: KClass<*>,
    private val records: Map<String, Record>
) {
  private val id: String = "$apolloClientId:${clazz.normalizedCacheName()}"
  fun id() = id

  fun displayName() = clazz.normalizedCacheName()

  fun recordCount() = records.count()

  fun records(): List<GraphQLRecord> = records.map { GraphQLRecord(it.value) }
}

@ApolloObject
@GraphQLName("Record")
internal class GraphQLRecord(
    private val record: Record,
) {
  fun key() = record.key

  fun fields() = record.fields

  fun size() = record.sizeInBytes
}

@ApolloAdapter
@GraphQLName(name = "Fields")
internal class FieldsAdapter : Adapter<Map<String, Any?>> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Map<String, Any?> {
    throw UnsupportedOperationException()
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Map<String, Any?>) {
    writer.writeAny(value)
  }
}

private fun KClass<*>.normalizedCacheName(): String = qualifiedName ?: toString()
