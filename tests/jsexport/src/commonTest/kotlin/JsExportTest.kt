import app.cash.turbine.test
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.apolloUnsafeCast
import com.apollographql.apollo3.api.json.ApolloJsonElement
import com.apollographql.apollo3.api.json.buildJsonString
import com.apollographql.apollo3.api.json.jsonReader
import com.apollographql.apollo3.api.json.readAny
import com.apollographql.apollo3.api.json.writeAny
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.TextMessage
import com.apollographql.apollo3.mockserver.WebSocketMessage
import com.apollographql.apollo3.mockserver.awaitWebSocketRequest
import com.apollographql.apollo3.mockserver.enqueueWebSocket
import com.apollographql.apollo3.network.ws.incubating.WebSocketNetworkTransport
import com.apollographql.apollo3.testing.internal.runTest
import jsexport.EventSubscription
import jsexport.GetAnimalQuery
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals


val response = """
          {
            "animal": {
              "__typename": "Cat",
              "name": "Noushka",
              "species": "Maine Coon"
            },
            "direction": "SOUTH",
            "point": {
              "x": 1,
              "y": 2
            },
            "bookOrLion": {
              "__typename": "Book",
              "title": "The Lion, the Witch and the Wardrobe"
            }
          }          
        """

expect fun data(response: String): GetAnimalQuery.Data

class JsExportTest {
  @Test
  fun test() {
    val data = data(response)

    assertEquals("Maine Coon", data.animal.species)
    assertEquals("Cat", data.animal.__typename)
    assertEquals("Noushka", data.animal.apolloUnsafeCast<GetAnimalQuery.Data.CatAnimal>().name)

    assertEquals("SOUTH", data.direction)

    assertEquals(1, data.point?.x)
    assertEquals(2, data.point?.y)
    assertEquals("The Lion, the Witch and the Wardrobe", data.bookOrLion?.apolloUnsafeCast<GetAnimalQuery.Data.BookBookOrLion>()?.title)
  }

  @Test
  fun testMockServer() = runTest {
    val mockServer = MockServer()

    val serverWriter = mockServer.enqueueWebSocket()

    val webSocketNetworkTransport = WebSocketNetworkTransport.Builder()
        .serverUrl(mockServer.url())
        .build()

    val request = ApolloRequest.Builder(EventSubscription())
        .build()

    webSocketNetworkTransport.execute(request).test {
      val serverReader = mockServer.awaitWebSocketRequest()
      serverReader.awaitMessage() 
      serverWriter.enqueueMessage(mapOf("type" to "connection_ack").toTextMessage())

      @Suppress("UNCHECKED_CAST")
      val subscribe = serverReader.awaitMessage().toApolloJsonElement() as Map<String, *>
      val id = subscribe["id"].toString()

      serverWriter.enqueueMessage(
          mapOf(
              "type" to "next",
              "id" to id,
              "payload" to mapOf(
                  "data" to mapOf(
                      "event" to mapOf(
                          "time" to "Jan 1st 1970",
                          "currentTimeMillis" to 42
                      )
                  )
              )
          ).toTextMessage())
      serverWriter.enqueueMessage(mapOf("type" to "complete", "id" to id).toTextMessage())

      awaitItem().data!!.apply {
        assertEquals("Jan 1st 1970", event?.time)
        assertEquals(42, event?.currentTimeMillis)
      }

      awaitComplete()
    }


    val data = data(response)

    assertEquals("Maine Coon", data.animal.species)
    assertEquals("Cat", data.animal.__typename)
    assertEquals("Noushka", data.animal.apolloUnsafeCast<GetAnimalQuery.Data.CatAnimal>().name)

    assertEquals("SOUTH", data.direction)

    assertEquals(1, data.point?.x)
    assertEquals(2, data.point?.y)
    assertEquals("The Lion, the Witch and the Wardrobe", data.bookOrLion?.apolloUnsafeCast<GetAnimalQuery.Data.BookBookOrLion>()?.title)
  }

  private fun ApolloJsonElement.toTextMessage(): TextMessage {
    return TextMessage(buildJsonString { writeAny(this@toTextMessage) })
  }

  private fun WebSocketMessage.toApolloJsonElement(): ApolloJsonElement {
    return Buffer().writeUtf8((this as TextMessage).text).jsonReader().readAny()
  }
}
