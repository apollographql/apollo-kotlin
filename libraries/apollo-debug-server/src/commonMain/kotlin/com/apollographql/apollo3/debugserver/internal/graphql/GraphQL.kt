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
import com.apollographql.apollo3.api.json.writeObject
import com.apollographql.apollo3.ast.toGQLDocument
import com.apollographql.apollo3.ast.toSchema
import com.apollographql.apollo3.cache.normalized.api.CacheKey
import com.apollographql.apollo3.cache.normalized.api.Record
import com.apollographql.apollo3.cache.normalized.apolloStore
import com.apollographql.apollo3.debugserver.internal.graphql.execution.ApolloDebugServerAdapterRegistry
import com.apollographql.apollo3.debugserver.internal.graphql.execution.ApolloDebugServerResolver
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

    ExecutableSchema.Builder()
        .schema(schema)
        .resolver(ApolloDebugServerResolver())
        .adapterRegistry(ApolloDebugServerAdapterRegistry)
        .build()
  }

  fun executeGraphQL(jsonBody: String): String {
    val graphQLRequestResult = Buffer().writeUtf8(jsonBody).parsePostGraphQLRequest()
    if (graphQLRequestResult is GraphQLRequestError) {
      return graphQLRequestResult.message
    }
    graphQLRequestResult as GraphQLRequest
    val dumps = apolloClients.mapValues { (apolloClient, _) ->
      runBlocking { apolloClient.apolloStore.dump() }
    }
    val apolloDebugContext = ApolloDebugContext(apolloClients, dumps)
    val graphQlResponse = executableSchema.execute(graphQLRequestResult, apolloDebugContext)
    val buffer = Buffer()
    graphQlResponse.serialize(buffer)
    return buffer.readUtf8()
  }

  internal class ApolloDebugContext(
      val apolloClients: Map<ApolloClient, String>,
      val dumps: Map<ApolloClient, Map<KClass<*>, Map<String, Record>>>,
  ) : ExecutionContext.Element {
    override val key: ExecutionContext.Key<ApolloDebugContext> = Key

    companion object Key : ExecutionContext.Key<ApolloDebugContext>
  }
}

@ApolloObject
internal class Query {
  private fun graphQLApolloClients(apolloDebugContext: GraphQL.ApolloDebugContext) =
      apolloDebugContext.apolloClients.map { (apolloClient, apolloClientId) ->
        GraphQLApolloClient(
            id = apolloClientId,
            displayName = apolloClientId,
            normalizedCaches = apolloDebugContext.dumps[apolloClient]!!.keys.map { clazz ->
              NormalizedCache(
                  id = "$apolloClientId:${clazz.normalizedCacheName()}",
                  displayName = clazz.normalizedCacheName(),
                  recordCount = apolloDebugContext.dumps[apolloClient]!![clazz]!!.size,
                  keyedRecords = apolloDebugContext.dumps[apolloClient]!![clazz]!!.map { (key, record) ->
                    KeyedRecord(
                        key = key,
                        record = record
                    )
                  }
              )
            }
        )
      }

  fun apolloClients(executionContext: ExecutionContext): List<GraphQLApolloClient> {
    val apolloDebugContext = executionContext[GraphQL.ApolloDebugContext]!!
    return graphQLApolloClients(apolloDebugContext)
  }

  fun apolloClient(executionContext: ExecutionContext, id: String): GraphQLApolloClient? {
    val apolloDebugContext = executionContext[GraphQL.ApolloDebugContext]!!
    return graphQLApolloClients(apolloDebugContext).firstOrNull { it.id() == id }
  }
}

@ApolloObject
@GraphQLName("ApolloClient")
internal class GraphQLApolloClient(
    private val id: String,
    private val displayName: String,
    private val normalizedCaches: List<NormalizedCache>,
) {
  fun id() = id

  fun displayName() = displayName

  fun normalizedCaches(): List<NormalizedCache> = normalizedCaches

  fun normalizedCache(id: String): NormalizedCache? {
    return normalizedCaches.firstOrNull { it.id() == id }
  }
}

@ApolloObject
internal class NormalizedCache(
    private val id: String,
    private val displayName: String,
    private val recordCount: Int,
    private val keyedRecords: List<KeyedRecord>,
) {
  fun id() = id

  fun displayName() = displayName

  fun recordCount() = recordCount

  fun keyedRecords(): List<KeyedRecord> = keyedRecords
}

@ApolloObject
internal class KeyedRecord(
    private val key: String,
    private val record: Record,
) {
  fun key() = key

  fun record() = record

  fun size() = record.sizeInBytes
}

@ApolloAdapter
@GraphQLName(name = "Record")
internal class RecordAdapter : Adapter<Record> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Record {
    throw UnsupportedOperationException()
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Record) {
    writer.writeObject {
      for ((k, v) in value.fields) {
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
