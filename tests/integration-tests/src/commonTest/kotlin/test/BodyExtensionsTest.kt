package test

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.http.DefaultHttpRequestComposer
import com.apollographql.apollo.api.http.HttpMethod
import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.HttpRequestComposer
import com.apollographql.apollo.api.json.jsonReader
import com.apollographql.apollo.api.json.readAny
import com.apollographql.apollo.api.json.writeObject
import com.apollographql.apollo.integration.fullstack.LaunchDetailsQuery
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.awaitRequest
import com.apollographql.mockserver.enqueueError
import com.apollographql.apollo.network.http.HttpNetworkTransport
import com.apollographql.apollo.testing.internal.runTest
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals

class WithExtensionsHttpRequestComposer(private val serverUrl: String) : HttpRequestComposer {
  override fun <D : Operation.Data> compose(apolloRequest: ApolloRequest<D>): HttpRequest {

    val request = HttpRequest.Builder(HttpMethod.Post, serverUrl)
        .body(
            DefaultHttpRequestComposer.buildPostBody(
                apolloRequest.operation,
                apolloRequest.executionContext[CustomScalarAdapters]!!,
                apolloRequest.operation.document(),
            ) {
              name("extensions")
              writeObject {
                name("key")
                value("value")
              }
            }
        )
        .build()

    return request
  }
}

class BodyExtensionsTest {
  @Suppress("UNCHECKED_CAST")
  @Test
  fun bodyExtensions() = runTest {
    val mockServer = MockServer()

    val apolloClient = ApolloClient.Builder()
        .networkTransport(
            HttpNetworkTransport.Builder()
                .httpRequestComposer(WithExtensionsHttpRequestComposer(mockServer.url()))
                .build()
        )
        .build()

    mockServer.enqueueError(statusCode = 500)
    apolloClient.query(LaunchDetailsQuery("42")).execute()

    val request = mockServer.awaitRequest()

    @Suppress("UNCHECKED_CAST")
    val asMap = Buffer().write(request.body).jsonReader().readAny() as Map<String, Any>
    assertEquals((asMap["extensions"] as Map<String, Any>).get("key"), "value")
    assertEquals((asMap["variables"] as Map<String, Any>).get("id"), "42")

    apolloClient.close()
    mockServer.close()
  }
}
