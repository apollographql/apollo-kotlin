package com.apollographql.apollo3.debugserver.internal.graphql

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.annotations.GraphQLAdapter
import com.apollographql.apollo3.annotations.GraphQLName
import com.apollographql.apollo3.annotations.GraphQLObject
import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.api.json.writeObject
import com.apollographql.apollo3.ast.toGQLDocument
import com.apollographql.apollo3.ast.toSchema
import com.apollographql.apollo3.cache.normalized.api.CacheKey
import com.apollographql.apollo3.cache.normalized.api.Record
import com.apollographql.apollo3.cache.normalized.apolloStore
import com.apollographql.apollo3.debugserver.internal.graphql.execution.ApolloDebugServerExecutableSchemaBuilder
import com.apollographql.apollo3.execution.ExecutableSchema
import com.apollographql.apollo3.execution.GraphQLRequest
import com.apollographql.apollo3.execution.GraphQLRequestError
import com.apollographql.apollo3.execution.parsePostGraphQLRequest
import okio.Buffer
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KClass

internal expect fun getExecutableSchema(): String

internal class GraphQL(
    private val apolloClients: AtomicReference<Map<ApolloClient, String>>,
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

@GraphQLObject
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

  fun apolloClient(id: String): GraphQLApolloClient? {
    return graphQLApolloClients().firstOrNull { it.id() == id }
  }
}

@GraphQLObject
@GraphQLName("ApolloClient")
internal class GraphQLApolloClient(
    private val id: String,
    private val apolloClient: ApolloClient,
) {
  fun id() = id

  fun displayName() = id

  fun normalizedCaches(): List<NormalizedCache> {
    val apolloStore = runCatching {  apolloClient.apolloStore }.getOrNull() ?: return emptyList()
    return apolloStore.dump().map {
      NormalizedCache(id, it.key, it.value)
    }
  }

  fun normalizedCache(id: String): NormalizedCache? {
    return normalizedCaches().firstOrNull { it.id() == id }
  }
}

@GraphQLObject
internal class NormalizedCache(
    apolloClientId: String,
    private val clazz: KClass<*>,
    private val records: Map<String, Record>,
) {
  private val id: String = "$apolloClientId:${clazz.normalizedCacheName()}"
  fun id() = id

  fun displayName() = clazz.normalizedCacheName()

  fun recordCount() = records.count()

  fun records(): List<GraphQLRecord> = records.map { GraphQLRecord(it.value) }
}

@GraphQLObject
@GraphQLName("Record")
internal class GraphQLRecord(
    private val record: Record,
) {
  fun key(): String = record.key

  fun fields(): Map<String, Any?> = record.fields

  fun sizeInBytes(): Int = record.sizeInBytes
}

@GraphQLAdapter(forScalar = "Fields")
internal class FieldsAdapter : Adapter<Map<String, Any?>> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Map<String, Any?> {
    throw UnsupportedOperationException()
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Map<String, Any?>) {
    writer.writeObject {
      for ((k, v) in value) {
        writer.name(k).writeJsonValue(v)
      }
    }
  }

  // Taken from JsonRecordSerializer
  @Suppress("UNCHECKED_CAST")
  private fun JsonWriter.writeJsonValue(value: Any?) {
    when (value) {
      null -> this.nullValue()
      is String -> this.value(value)
      is Boolean -> this.value(value)
      is Int -> this.value(value)
      is Long -> this.value(value)
      is Double -> this.value(value)
      is CacheKey -> this.value(value.serialize())
      is List<*> -> {
        this.beginArray()
        value.forEach { writeJsonValue(it) }
        this.endArray()
      }

      is Map<*, *> -> {
        this.beginObject()
        for (entry in value as Map<String, Any?>) {
          this.name(entry.key).writeJsonValue(entry.value)
        }
        this.endObject()
      }

      else -> error("Unsupported record value type: '$value'")
    }
  }
}

private fun KClass<*>.normalizedCacheName(): String = qualifiedName ?: toString()
