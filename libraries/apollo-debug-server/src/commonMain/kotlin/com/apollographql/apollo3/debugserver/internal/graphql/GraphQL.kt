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
import okio.Buffer
import kotlin.reflect.KClass

internal expect fun KClass<*>.normalizedCacheName(): String

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

  suspend fun executeGraphQL(jsonBody: String): String {
    val graphQLRequestResult = Buffer().writeUtf8(jsonBody).parsePostGraphQLRequest()
    if (graphQLRequestResult is GraphQLRequestError) {
      return graphQLRequestResult.message
    }
    graphQLRequestResult as GraphQLRequest
    val dumps = apolloClients.mapValues { (apolloClient, _) ->
      apolloClient.apolloStore.dump()
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
  fun version() = 1

  fun clients(executionContext: ExecutionContext): List<Client> {
    val apolloDebugContext = executionContext[GraphQL.ApolloDebugContext]!!
    return apolloDebugContext.apolloClients.map { (apolloClient, id) ->
      Client(
          id = id,
          displayName = id,
          normalizedCacheInfos = apolloDebugContext.dumps[apolloClient]!!.keys.map { clazz ->
            NormalizedCacheInfo(
                id = "$id:${clazz.normalizedCacheName()}",
                displayName = clazz.normalizedCacheName(),
                recordCount = apolloDebugContext.dumps[apolloClient]!![clazz]!!.size
            )
          }
      )
    }
  }

  fun normalizedCache(executionContext: ExecutionContext, id: String): NormalizedCache {
    val apolloDebugContext = executionContext[GraphQL.ApolloDebugContext]!!
    val clientId = id.substringBeforeLast(":")
    val cacheId = id.substringAfterLast(":")
    val entry = apolloDebugContext.apolloClients.entries.firstOrNull { it.value == clientId }
    val apolloClient = entry?.key
    if (apolloClient == null) {
      error("Unknown client '$clientId'")
    } else {
      val cache = apolloDebugContext.dumps[apolloClient]!!.entries.firstOrNull { it.key.normalizedCacheName() == cacheId }?.value
      if (cache == null) {
        error("Unknown cache '$cacheId' for client '$clientId'")
      } else {
        return NormalizedCache(
            id = id,
            displayName = cacheId,
            clientDisplayName = entry.value,
            records = cache.map { (key, record) ->
              KeyedRecord(
                  key = key,
                  record = record
              )
            }
        )
      }
    }
  }
}

@ApolloObject
internal class Client(
    private val id: String,
    private val displayName: String,
    private val normalizedCacheInfos: List<NormalizedCacheInfo>,
) {
  fun id() = id

  fun displayName() = displayName

  fun normalizedCacheInfos(): List<NormalizedCacheInfo> = normalizedCacheInfos
}

@ApolloObject
internal class NormalizedCacheInfo(
    private val id: String,
    private val displayName: String,
    private val recordCount: Int,
) {
  fun id() = id

  fun displayName() = displayName

  fun recordCount() = recordCount
}

@ApolloObject
internal class NormalizedCache(
    private val id: String,
    private val displayName: String,
    private val clientDisplayName: String,
    private val records: List<KeyedRecord>,
) {
  fun id() = id

  fun displayName() = displayName

  fun clientDisplayName() = clientDisplayName

  fun records(): List<KeyedRecord> = records
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
