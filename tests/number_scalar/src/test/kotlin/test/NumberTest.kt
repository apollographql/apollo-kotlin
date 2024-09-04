package test


import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Adapter
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.json.JsonNumber
import com.apollographql.apollo.api.json.JsonReader
import com.apollographql.apollo.api.json.JsonWriter
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.apollographql.apollo.cache.normalized.normalizedCache
import com.apollographql.apollo.testing.QueueTestNetworkTransport
import com.apollographql.apollo.testing.enqueueTestResponse
import com.example.GetNumberQuery
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class NumberTest {
  @Test
  fun test(): Unit = runBlocking {
    ApolloClient.Builder()
        .networkTransport(QueueTestNetworkTransport())
        .normalizedCache(MemoryCacheFactory())
        .addCustomScalarAdapter(com.example.type.Number.type, NumberAdapter())
        .build()
        .use { apolloClient ->
          apolloClient.enqueueTestResponse(GetNumberQuery(), GetNumberQuery.Data("12345"))
          apolloClient.query(GetNumberQuery()).fetchPolicy(FetchPolicy.NetworkOnly).execute().apply {
            assertEquals("12345", data?.number)
          }
          apolloClient.query(GetNumberQuery()).fetchPolicy(FetchPolicy.CacheOnly).execute().apply {
            assertEquals("12345", data?.number)
          }
        }
  }
}

class NumberAdapter: Adapter<String> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): String {
    return reader.nextNumber().value
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: String) {
    writer.value(JsonNumber(value))
  }
}
