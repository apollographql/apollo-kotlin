package com.apollographql.apollo3.integration

import com.apollographql.apollo3.api.Executable
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.MergedField
import com.apollographql.apollo3.api.composeResponseBody
import com.apollographql.apollo3.cache.normalized.CacheKey
import com.apollographql.apollo3.cache.normalized.CacheKeyResolver
import com.apollographql.apollo3.mockserver.MockResponse
import com.apollographql.apollo3.mockserver.MockServer
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeout
import okio.ByteString.Companion.encodeUtf8
import kotlin.test.assertEquals


fun <D : Operation.Data> MockServer.enqueue(
    operation: Operation<D>,
    data: D,
    customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty
) {
  val json = operation.composeResponseBody(data, customScalarAdapters = customScalarAdapters)
  enqueue(json)
}

fun MockServer.enqueue(string: String) {
  val byteString = string.encodeUtf8()
  enqueue(MockResponse(
      statusCode = 200,
      headers = mapOf("Content-Length" to byteString.size.toString()),
      body = byteString
  ))
}

fun readTestFixture(name: String) = readFile("../integration-tests/testFixtures/$name")
fun readResource(name: String) = readFile("../integration-tests/testFixtures/resources/$name")

object IdFieldCacheKeyResolver : CacheKeyResolver() {
  override fun fromFieldRecordSet(field: MergedField, variables: Executable.Variables, recordSet: Map<String, Any?>): CacheKey {
    val id = recordSet["id"]
    return if (id != null) {
      formatCacheKey(id.toString())
    } else {
      formatCacheKey(null)
    }
  }

  override fun fromFieldArguments(field: MergedField, variables: Executable.Variables): CacheKey {
    val id = field.resolveArgument("id", variables)
    return if (id != null) {
      formatCacheKey(id.toString())
    } else {
      formatCacheKey(null)
    }
  }

  private fun formatCacheKey(id: String?): CacheKey {
    return if (id == null || id.isEmpty()) {
      CacheKey.NO_KEY
    } else {
      CacheKey(id)
    }
  }
}

suspend fun <T> Channel<T>.receiveOrTimeout(timeoutMillis: Long = 500) = withTimeout(timeoutMillis) {
  receive()
}

/**
 * A helper function to reverse the order of the argument so that we can easily column edit the tests
 */
fun assertEquals2(actual: Any?, expected: Any?) = assertEquals(expected, actual)
