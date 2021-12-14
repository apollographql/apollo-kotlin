package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.http.ByteStringHttpBody
import com.apollographql.apollo3.api.http.HttpMethod
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.HttpRequestComposer
import com.apollographql.apollo3.api.json.buildJsonByteString
import com.apollographql.apollo3.api.json.writeObject
import com.apollographql.apollo3.api.json.jsonReader
import com.apollographql.apollo3.api.json.readAny
import com.apollographql.apollo3.integration.normalizer.HeroNameQuery
import com.apollographql.apollo3.mockserver.MockResponse
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.network.http.HttpNetworkTransport
import com.apollographql.apollo3.testing.runTest
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ApolloInternal::class)
class WithExtensionsHttpRequestComposer(private val serverUrl: String): HttpRequestComposer {
  override fun <D : Operation.Data> compose(apolloRequest: ApolloRequest<D>): HttpRequest {

    return HttpRequest.Builder(HttpMethod.Post, serverUrl)
        .body(
            ByteStringHttpBody(
                "application/json",
                buildJsonByteString {
                  writeObject {
                    name("query")
                    value(apolloRequest.operation.document())
                    name("operationName")
                    value(apolloRequest.operation.name())
                    name("extensions")
                    value("extension value")
                  }
                }
            )
        )
        .build()
  }
}

@OptIn(ApolloInternal::class)
class BodyExtensionsTest {
  @Test
  fun bodyExtensions() = runTest {
    val mockServer = MockServer()
    val serverUrl = mockServer.url()

    val apolloClient = ApolloClient.Builder()
        .networkTransport(
            HttpNetworkTransport.Builder()
                .httpRequestComposer(WithExtensionsHttpRequestComposer(serverUrl))
                .build()
        )
        .build()

    mockServer.enqueue(MockResponse(statusCode = 500))
    kotlin.runCatching {
      apolloClient.query(HeroNameQuery())
          .execute()
    }

    val request = mockServer.takeRequest()
    val asMap = Buffer().write(request.body).jsonReader().readAny() as Map<String, Any>
    assertEquals(asMap["extensions"], "extension value")

    apolloClient.dispose()
    mockServer.stop()
  }
}
