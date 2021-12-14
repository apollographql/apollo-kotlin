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
import com.apollographql.apollo3.mockserver.MockResponse
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.network.http.HttpNetworkTransport
import com.apollographql.apollo3.testing.runTest
import okio.Buffer
import org.junit.Test
import reserved.GetRandomQuery
import kotlin.test.assertEquals

@ApolloInternal
class NoQueryDocumentTest {
  private val queryDocument = """
    query GetRandomQuery {
      random
    }
  """.trimIndent()
  
  @Test
  fun noQueryDocumentTest() = runTest {
    val mockServer = MockServer()
    val serverUrl = mockServer.url()

    val composer = object : HttpRequestComposer {
      override fun <D : Operation.Data> compose(apolloRequest: ApolloRequest<D>): HttpRequest {

        return HttpRequest.Builder(HttpMethod.Post, serverUrl)
            .body(
                ByteStringHttpBody(
                    "application/json",
                    buildJsonByteString {
                      writeObject {
                        name("query")
                        value(queryDocument)
                        name("operationName")
                        value("GetRandomQuery")
                      }
                    }
                )
            )
            .build()
      }
    }

    val apolloClient = ApolloClient.Builder()
        .networkTransport(
            HttpNetworkTransport.Builder()
                .httpRequestComposer(composer)
                .build()
        )
        .build()

    mockServer.enqueue(MockResponse(statusCode = 500))
    kotlin.runCatching {
      apolloClient.query(GetRandomQuery())
          .execute()
    }

    val request = mockServer.takeRequest()
    val asMap = Buffer().write(request.body).jsonReader().readAny() as Map<String, Any>
    assertEquals(asMap["query"], queryDocument)

    apolloClient.dispose()
    mockServer.stop()
  }
}
