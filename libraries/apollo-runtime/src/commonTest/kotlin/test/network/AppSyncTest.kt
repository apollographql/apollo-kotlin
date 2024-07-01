package test.network

import com.apollographql.apollo.network.websocket.AppSyncWsProtocol
import kotlin.test.Test
import kotlin.test.assertEquals

class AppSyncTest {
  private val authorization = mapOf(
    "host" to "example1234567890000.appsync-api.us-east-1.amazonaws.com",
    "x-api-key" to "da2-12345678901234567890123456"
  )

  @Test
  fun buildUrl() {
    val url = AppSyncWsProtocol.buildUrl(
        baseUrl = "wss://example1234567890000.appsync-realtime-api.us-east-1.amazonaws.com/graphql",
        authorization = authorization
    )
    assertEquals(
        url,
        "wss://example1234567890000.appsync-realtime-api.us-east-1.amazonaws.com/graphql?header=eyJob3N0IjoiZXhhbXBsZTEyMzQ1Njc4OTAwMDAuYXBwc3luYy1hcGkudXMtZWFzdC0xLmFtYXpvbmF3cy5jb20iLCJ4LWFwaS1rZXkiOiJkYTItMTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTYifQ%3D%3D&payload=e30%3D"
    )
  }
}