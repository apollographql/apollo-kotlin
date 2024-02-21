package com.apollographql.apollo.sample.server

import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.ast.toGQLDocument
import com.apollographql.apollo3.ast.toSchema
import com.apollographql.apollo3.execution.ExecutableSchema
import com.apollographql.apollo3.execution.GraphQLRequest
import com.apollographql.apollo3.execution.GraphQLRequestError
import com.apollographql.apollo3.execution.WebSocketBinaryMessage
import com.apollographql.apollo3.execution.WebSocketMessage
import com.apollographql.apollo3.execution.WebSocketTextMessage
import com.apollographql.apollo3.execution.parseGetGraphQLRequest
import com.apollographql.apollo3.execution.parsePostGraphQLRequest
import com.apollographql.apollo3.execution.websocket.ApolloWebSocketHandler
import com.apollographql.apollo3.execution.websocket.ConnectionInitAck
import com.apollographql.apollo3.execution.websocket.ConnectionInitError
import com.apollographql.apollo3.execution.websocket.ConnectionInitHandler
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okio.Buffer
import okio.buffer
import okio.source
import okio.withLock
import org.http4k.core.HttpHandler
import org.http4k.core.MemoryBody
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
import sample.server.execution.SampleserverExecutableSchemaBuilder
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

  return SampleserverExecutableSchemaBuilder(schema)
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
      return Response(BAD_REQUEST).body(graphQLRequestResult.message)
    }
    graphQLRequestResult as GraphQLRequest

    val response = executableSchema.execute(graphQLRequestResult, executionContext)

    val buffer = Buffer()
    response.serialize(buffer)
    val responseText = buffer.readUtf8()

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

class ActiveWebSocket(val websocket: Websocket, val messages: MutableList<String>)

interface MessageList {
  fun add(message: String)
}

class WebSocketRegistry : ExecutionContext.Element {
  private val activeSockets = mutableListOf<ActiveWebSocket>()
  private val lock = reentrantLock()

  fun getAllMessages(): List<String> {
    return lock.withLock {
      activeSockets.flatMap { it.messages }
    }
  }

  fun rememberWebSocket(ws: Websocket): MessageList {
    val aw = lock.withLock {
      ActiveWebSocket(ws, mutableListOf()).also {
        activeSockets.add(it)
      }
    }

    return object : MessageList {
      override fun add(message: String) {
        lock.withLock {
          aw.messages.add(message)
        }
      }
    }
  }

  fun forgetWebSocket(ws: Websocket) {
    lock.withLock {
      activeSockets.removeIf { it.websocket === ws }
    }
  }

  fun closeAllWebSockets(): Int {
    return lock.withLock {
      var count = 0
      activeSockets.toList().forEach {
        it.websocket.close(WsStatus(1011, "closed"))
        count++
      }
      count
    }
  }

  override val key: ExecutionContext.Key<WebSocketRegistry> = Key

  companion object Key : ExecutionContext.Key<WebSocketRegistry>
}

class CurrentWebSocket(val ws: Websocket) : ExecutionContext.Element {

  override val key: ExecutionContext.Key<CurrentWebSocket> = Key

  companion object Key : ExecutionContext.Key<CurrentWebSocket>
}

fun ApolloWebsocketHandler(executableSchema: ExecutableSchema, webSocketRegistry: WebSocketRegistry): WsHandler {

  val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

  return { _: Request ->
    WsResponse { ws: Websocket ->
      val messageList = webSocketRegistry.rememberWebSocket(ws)

      val connectionInitHandler: ConnectionInitHandler = { connectionParams: Any? ->
        @Suppress("UNCHECKED_CAST")
        val shouldReturn = (connectionParams as? Map<String, Any?>)?.get("return")?.toString()

        when {
          shouldReturn == "error" -> {
            ConnectionInitError()
          }

          shouldReturn != null && shouldReturn.toString().startsWith("close") -> {
            val code = Regex("close\\(([0-9]*)\\)").matchEntire(shouldReturn)
                ?.let { it.groupValues[1].toIntOrNull() }
                ?: 1001

            ws.close(WsStatus(code, "closed"))

            ConnectionInitError()
          }

          else -> {
            ConnectionInitAck
          }
        }
      }

      val sendMessage = { webSocketMessage: WebSocketMessage ->
        ws.send(webSocketMessage.toWsMessage())
      }

      val handler = ApolloWebSocketHandler(
          executableSchema = executableSchema,
          scope = scope,
          executionContext = webSocketRegistry + CurrentWebSocket(ws),
          sendMessage = sendMessage,
          connectionInitHandler = connectionInitHandler
      )

      ws.onMessage {
        it.body.payload.array().decodeToString().let {
          handler.handleMessage(WebSocketTextMessage(it))
          messageList.add(it)
        }
      }
      ws.onClose {
        handler.close()
        webSocketRegistry.forgetWebSocket(ws)
      }
      ws.onError {
        handler.close()
        webSocketRegistry.forgetWebSocket(ws)
      }
    }
  }
}

private fun WebSocketMessage.toWsMessage(): WsMessage {
  return when (this) {
    is WebSocketBinaryMessage -> {
      WsMessage(MemoryBody(data))
    }

    is WebSocketTextMessage -> {
      WsMessage(data)
    }
  }
}

fun AppHandler(): PolyHandler {
  val executableSchema = ExecutableSchema()
  val forceClose = WebSocketRegistry()
  val ws = websockets("/subscriptions" wsBind ApolloWebsocketHandler(executableSchema, webSocketRegistry = forceClose))

  val http = ServerFilters.CatchAll {
    it.printStackTrace()
    ServerFilters.CatchAll.originalBehaviour(it)
  }
      .then(ServerFilters.Cors(UnsafeGlobalPermissive))
      .then(
          routes(
              "/graphql" bind Method.POST to GraphQLHttpHandler(executableSchema, forceClose),
              "/graphql" bind Method.GET to GraphQLHttpHandler(executableSchema, forceClose),
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