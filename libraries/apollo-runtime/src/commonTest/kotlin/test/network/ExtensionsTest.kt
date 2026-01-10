package test.network

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.annotations.ApolloInternal
import com.apollographql.apollo.api.http.HttpMethod
import com.apollographql.apollo.api.json.ApolloJsonElement
import com.apollographql.apollo.api.json.jsonReader
import com.apollographql.apollo.api.json.readAny
import com.apollographql.apollo.testing.internal.runTest
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.enqueueString
import okio.Buffer
import okio.use
import test.FooQuery
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class ExtensionsTest {
  @OptIn(ApolloInternal::class)
  @Suppress("UNCHECKED_CAST")
  @Test
  fun testExtensions() = runTest {
    MockServer().use { mockServer ->
      ApolloClient.Builder()
          .serverUrl(mockServer.url())
          .build()
          .use { apolloClient ->
            mockServer.enqueueString(FooQuery.successResponse)
            apolloClient.query(FooQuery()).extensions(mapOf("foo" to "bar")).execute()
            val asMap = Buffer().write(mockServer.takeRequest().body).jsonReader().readAny() as Map<String, ApolloJsonElement>
            assertEquals((asMap["extensions"] as Map<String, ApolloJsonElement>).get("foo"), "bar")

            mockServer.enqueueString(FooQuery.successResponse)
            apolloClient.query(FooQuery()).extensions(mapOf("foo" to "bar")).httpMethod(HttpMethod.Get).execute()
            assertContains(mockServer.takeRequest().path, "%22foo%22%3A%22bar%22")
          }
    }
  }
}

