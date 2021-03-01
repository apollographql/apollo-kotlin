package com.apollographql.apollo3.integration.test

import HeroNameQuery
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.exception.ApolloHttpException
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.apollographql.apollo3.integration.TestApolloClient
import com.apollographql.apollo3.network.http.HttpResponse
import com.apollographql.apollo3.testing.TestHttpEngine
import com.apollographql.apollo3.testing.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExceptionsTest {
  private lateinit var testHttpEngine: TestHttpEngine
  private lateinit var apolloClient: ApolloClient

  @BeforeTest
  fun setUp() {
    testHttpEngine = TestHttpEngine()
    apolloClient = TestApolloClient(testHttpEngine)
  }

  @Test
  fun `when query and malformed network response, assert Exception`() = runBlocking {
    testHttpEngine.enqueue("malformed")

    val result = kotlin.runCatching {
      apolloClient
          .query(HeroNameQuery())
          .single()
    }

    assertTrue(result.exceptionOrNull() != null)
  }

  @Test
  fun `when http error, assert execute fails`() = runBlocking {
    testHttpEngine.enqueue(
        HttpResponse(
            statusCode = 404,
            headers = emptyMap(),
            body = null
        )
    )

    val result = kotlin.runCatching {
      apolloClient
          .query(HeroNameQuery())
          .single()
    }

    val exception = result.exceptionOrNull()
    assertTrue(exception is ApolloHttpException)
    assertEquals(404, exception.statusCode)
  }

  @Test
  fun `when network error, assert ApolloNetworkException`() = runBlocking {
    testHttpEngine.enqueue {
      throw ApolloNetworkException()
    }

    val result = kotlin.runCatching {
      apolloClient
          .query(HeroNameQuery())
          .single()
    }

    val exception = result.exceptionOrNull()
    assertTrue(exception is ApolloNetworkException)
  }

  @Test
  fun `when query and malformed network response, assert success after retry`() {
    testHttpEngine.enqueue("")
    val query = HeroNameQuery()
    val data = HeroNameQuery.Data(HeroNameQuery.Data.Hero("R2-D2"))
    testHttpEngine.enqueue(query, data)

    val response = runBlocking {
      apolloClient
          .query(query)
          .retryWhen { _, attempt -> attempt == 0L }
          .single()
    }

    assertEquals(data, response.data)
  }
}