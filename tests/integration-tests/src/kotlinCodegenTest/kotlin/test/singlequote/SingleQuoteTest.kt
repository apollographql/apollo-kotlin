package test.singlequote

import com.apollographql.apollo3.api.http.HttpMethod
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.mockServerTest
import singlequote.IsAvailableQuery
import kotlin.test.Test

class SingleQuoteTest {

  @Test
  fun failsWithAnnotationInQueryWithPost() = mockServerTest{
    mockServer.enqueue("""
      {
        "data": { "isUsernameAvailable": true } 
      }
    """.trimIndent())

    val response = apolloClient.query(IsAvailableQuery("o'reilly")).execute()

    check(response.dataOrThrow().isUsernameAvailable)
  }

  @Test
  fun failsWithAnnotationInQueryWithGet() = mockServerTest{
    mockServer.enqueue("""
      {
        "data": { "isUsernameAvailable": true } 
      }
    """.trimIndent())

    val response = apolloClient.query(IsAvailableQuery("o'reilly"))
        .httpMethod(HttpMethod.Get)
        .execute()

    check(response.dataOrThrow().isUsernameAvailable)
  }

}
