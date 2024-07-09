package test

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.annotations.ApolloInternal
import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.http.ByteStringHttpBody
import com.apollographql.apollo.api.http.HttpMethod
import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.HttpRequestComposer
import com.apollographql.apollo.api.json.buildJsonByteString
import com.apollographql.apollo.api.json.jsonReader
import com.apollographql.apollo.api.json.readAny
import com.apollographql.apollo.api.json.writeObject
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.awaitRequest
import com.apollographql.mockserver.enqueueError
import com.apollographql.mockserver.enqueueString
import com.apollographql.apollo.network.http.HttpNetworkTransport
import com.apollographql.apollo.testing.internal.runTest
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

    mockServer.enqueueError(statusCode = 500)
    kotlin.runCatching {
      apolloClient.query(GetRandomQuery())
          .execute()
    }

    val request = mockServer.awaitRequest()

    @Suppress("UNCHECKED_CAST")
    val asMap = Buffer().write(request.body).jsonReader().readAny() as Map<String, Any>
    assertEquals(asMap["query"], queryDocument)

    apolloClient.close()
    mockServer.close()
  }
}
