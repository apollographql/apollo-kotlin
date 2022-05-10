package com.apollographql.apollo.sample.server.routing

import com.apollographql.apollo.sample.server.sse.SseSideChannelInteractor
import com.apollographql.apollo3.network.sse.SseTransportMessage
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respondText
import io.ktor.routing.Routing
import io.ktor.routing.post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SseSideChannelRouter(private val interactor: SseSideChannelInteractor = SseSideChannelInteractor()) : Router() {
  companion object {
    const val SIDE_CHANNEL_PATH = "v1/subscription"
  }

  override fun routing(routing: Routing) {
    routing.post("/$SIDE_CHANNEL_PATH") {

      withContext(Dispatchers.IO) {
        try {
          call.receive<SseTransportMessage.ClientRequest>()
              .let { interactor.processRequest(it) }
              .let { serialize(it) }
              .let {
                call.respondText(
                    text = it,
                    contentType = ContentType.Application.Json,
                    status = HttpStatusCode.Created,
                )
              }


        } catch (t: Throwable) {
          // TODO handle error as part of MOBPLAT-1547
          t.printStackTrace()
        }
      }
    }
  }

  private fun serialize(response: SseTransportMessage.ClientResponse): String {

    return Json.encodeToString(response)

  }

}
