package com.apollographql.apollo.sample.server

import com.apollographql.apollo3.api.Error
import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.ast.toGQLDocument
import com.apollographql.apollo3.ast.toSchema
import com.apollographql.apollo3.execution.ApolloWebsocketClientMessageParseError
import com.apollographql.apollo3.execution.ApolloWebsocketComplete
import com.apollographql.apollo3.execution.ApolloWebsocketConnectionAck
import com.apollographql.apollo3.execution.ApolloWebsocketConnectionError
import com.apollographql.apollo3.execution.ApolloWebsocketData
import com.apollographql.apollo3.execution.ApolloWebsocketError
import com.apollographql.apollo3.execution.ApolloWebsocketInit
import com.apollographql.apollo3.execution.ApolloWebsocketServerMessage
import com.apollographql.apollo3.execution.ApolloWebsocketStart
import com.apollographql.apollo3.execution.ApolloWebsocketStop
import com.apollographql.apollo3.execution.ApolloWebsocketTerminate
import com.apollographql.apollo3.execution.ExecutableSchema
import com.apollographql.apollo3.execution.GraphQLRequest
import com.apollographql.apollo3.execution.GraphQLRequestError
import com.apollographql.apollo3.execution.SubscriptionItemError
import com.apollographql.apollo3.execution.SubscriptionItemResponse
import com.apollographql.apollo3.execution.parseApolloWebsocketClientMessage
import com.apollographql.apollo3.execution.parseGetGraphQLRequest
import com.apollographql.apollo3.execution.parsePostGraphQLRequest
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okio.Buffer
import okio.buffer
import okio.source
import okio.withLock
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.CorsPolicy.Companion.UnsafeGlobalPermissive
import org.http4k.filter.ServerFilters
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.routing.websockets
import org.http4k.server.Jetty
import org.http4k.server.PolyHandler
import org.http4k.server.ServerConfig
import org.http4k.server.asServer
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsHandler
import org.http4k.websocket.WsMessage
import org.http4k.websocket.WsResponse
import org.http4k.websocket.WsStatus
import sample.server.execution.SampleserverAdapterRegistry
import sample.server.execution.SampleserverResolver
import java.io.Closeable
import java.time.Duration
import org.http4k.routing.ws.bind as wsBind

fun ExecutableSchema(): ExecutableSchema {
  val schema = GraphQLHttpHandler::class.java.classLoader
      .getResourceAsStream("schema.graphqls")!!
      .source()
      .buffer()
      .readUtf8()
      .toGQLDocument()
      .toSchema()

  return ExecutableSchema.Builder()
      .schema(schema)
      .resolver(SampleserverResolver())
      .adapterRegistry(SampleserverAdapterRegistry)
      .build()
}

class GraphQLHttpHandler(val executableSchema: ExecutableSchema, val executionContext: ExecutionContext) : HttpHandler {
  override fun invoke(request: Request): Response {

    val graphQLRequestResult = when (request.method) {
      Method.GET -> request.uri.toString().parseGetGraphQLRequest()
      Method.POST -> request.body.stream.source().buffer().use { it.parsePostGraphQLRequest() }
      else -> error("")
    }

    if (graphQLRequestResult is GraphQLRequestError) {
      println("Got Request: ${graphQLRequestResult.message}")
      return Response(BAD_REQUEST).body(graphQLRequestResult.message)
    }
    graphQLRequestResult as GraphQLRequest

    println("Got Request")
    println("document=${graphQLRequestResult.document}")
    println("variables=${graphQLRequestResult.variables}")

    val response = executableSchema.execute(graphQLRequestResult, executionContext)

    val buffer = Buffer()
    response.serialize(buffer)
    val responseText = buffer.readUtf8()
    println("response: $responseText")

    return Response(OK)
        .header("content-type", "application/json")
        .body(responseText)
  }
}

class SandboxHandler : HttpHandler {
  override fun invoke(request: Request): Response {
    return Response(OK).body(javaClass.classLoader!!.getResourceAsStream("sandbox.html")!!)
  }
}

private fun ApolloWebsocketServerMessage.toWsMessage(): WsMessage {
  // XXX: avoid the encoding/decoding to utf8
  return WsMessage(Buffer().apply { serialize(this) }.readUtf8())
}


class WebsocketSession {
  val subscriptions = mutableMapOf<String, Job>()
}


class WebsocketRegistry : ExecutionContext.Element {
  private val lock = reentrantLock()

  private val sessions = mutableMapOf<Websocket, WebsocketSession>()

  fun startSession(ws: Websocket) {
    lock.withLock {
      check(sessions.put(ws, WebsocketSession()) == null) {
        "This session was already started"
      }
    }
  }

  fun stopSession(ws: Websocket) {
    lock.withLock {
      val session = sessions.remove(ws)
      if (session == null) {
        // Session already removed
        // Might happen because we call this both from onError and onClose, so we need to be idempotent
        return@withLock
      }

      session.subscriptions.forEach {
        it.value.cancel()
      }
    }
  }

  fun addSubscription(ws: Websocket, id: String, job: Job) {
    lock.withLock {
      sessions.get(ws)!!.subscriptions.put(id, job)
    }
  }

  fun removeSubscription(ws: Websocket, id: String): Boolean {
    val removed = lock.withLock {
      sessions.get(ws)!!.subscriptions.remove(id)
    }

    removed?.cancel()

    return removed != null
  }

  fun closeAllSessions(): Int {

    var count = 0
    lock.withLock {
      // Create a copy to avoid concurrent modification
      sessions.keys.toList().forEach {
        // Close should trigger the onClose call and remove from the `sessions`
        it.close(WsStatus(1011, "closed"))
        count++
      }
    }
    return count
  }

  override val key: ExecutionContext.Key<WebsocketRegistry>
    get() = Key

  companion object Key : ExecutionContext.Key<WebsocketRegistry>
}

fun ApolloWebsocketHandler(executableSchema: ExecutableSchema, websocketRegistry: WebsocketRegistry): WsHandler {

  val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

  return { _: Request ->
    WsResponse { ws: Websocket ->

      websocketRegistry.startSession(ws)

      ws.onMessage {
        when (val message = String(it.body.payload.array()).parseApolloWebsocketClientMessage()) {
          is ApolloWebsocketInit -> {
            @Suppress("UNCHECKED_CAST")
            val shouldReturn = (message.connectionParams as? Map<String, Any?>)?.get("return")?.toString()

            when {
              shouldReturn == "error" -> {
                ws.send(ApolloWebsocketConnectionError().toWsMessage())
              }

              shouldReturn != null && shouldReturn.toString().startsWith("close") -> {
                val code = Regex("close\\(([0-9]*)\\)").matchEntire(shouldReturn)
                    ?.let { it.groupValues[1].toIntOrNull() }
                    ?: 1001

                ws.close(WsStatus(code, "closed"))
              }
              else -> {
                ws.send(ApolloWebsocketConnectionAck.toWsMessage())
              }
            }
          }

          is ApolloWebsocketStart -> {
            val flow = executableSchema.executeSubscription(message.request, ExecutionContext.Empty)

            val job = scope.launch {
              flow.collect {
                when (it) {
                  is SubscriptionItemResponse -> {
                    ws.send(ApolloWebsocketData(id = message.id, response = it.response).toWsMessage())
                  }

                  is SubscriptionItemError -> {
                    ws.send(ApolloWebsocketError(id = message.id, error = it.error).toWsMessage())
                  }
                }
              }
              ws.send(ApolloWebsocketComplete(id = message.id).toWsMessage())
              websocketRegistry.removeSubscription(ws, message.id)
            }
            websocketRegistry.addSubscription(ws, message.id, job)
          }

          is ApolloWebsocketStop -> {
            if (!websocketRegistry.removeSubscription(ws, message.id)) {
              ws.send(ApolloWebsocketError(message.id, Error.Builder("No active subscription found for '${message.id}'").build()).toWsMessage())
            }
          }

          ApolloWebsocketTerminate -> {
            // nothing to do
          }

          is ApolloWebsocketClientMessageParseError -> {
            ws.send(ApolloWebsocketError(null, Error.Builder("Cannot handle message (${message.message})").build()).toWsMessage())
          }
        }
        ws.onClose {
          websocketRegistry.stopSession(ws)
        }
        ws.onError {
          websocketRegistry.stopSession(ws)
        }
      }
    }
  }
}

fun AppHandler(): PolyHandler {
  val executableSchema = ExecutableSchema()
  val websocketRegistry = WebsocketRegistry()
  val ws = websockets("/subscriptions" wsBind ApolloWebsocketHandler(executableSchema, websocketRegistry))

  val http = ServerFilters.CatchAll {
    it.printStackTrace()
    ServerFilters.CatchAll.originalBehaviour(it)
  }
      .then(ServerFilters.Cors(UnsafeGlobalPermissive))
      .then(
          routes(
              "/graphql" bind Method.POST to GraphQLHttpHandler(executableSchema, websocketRegistry),
              "/graphql" bind Method.GET to GraphQLHttpHandler(executableSchema, websocketRegistry),
              "/sandbox" bind Method.GET to SandboxHandler()
          )
      )


  return PolyHandler(http, ws)
}

fun runServer() {
  AppHandler().asServer(Jetty(8000)).start().block()
}

class SampleServer(val port: Int = 0) : Closeable {
  private val server = AppHandler().asServer(Jetty(port, stopMode = ServerConfig.StopMode.Graceful(Duration.ZERO)))

  init {
    server.start()
  }

  fun graphqlUrl(): String {
    return "http://localhost:${server.port()}/graphql"
  }

  fun subscriptionsUrl(): String {
    return "http://localhost:${server.port()}/subscriptions"
  }

  override fun close() {
    server.close()
  }
}