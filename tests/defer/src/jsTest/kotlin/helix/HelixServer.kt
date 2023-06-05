package helix

import express.express
import graphql.GraphQLSchema
import graphql.execute
import graphql.subscribe
import graphqlws.useServer.useServer
import util.dynamicObject
import ws.Server
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class HelixServer(
    private val schema: GraphQLSchema,
    private val port: Int = 4000,
) {
  private var server: dynamic = null
  private var wsServer: dynamic = null
  private var url: String? = null
  private var webSocketUrl: String? = null

  // Based on https://github.com/contra/graphql-helix/blob/main/examples/graphql-ws/server.ts
  init {
    val app = express()

    app.use(express.json())
    app.use("/graphql") { req: dynamic, res: dynamic ->
      val request = dynamicObject {
        body = req.body
        headers = req.headers
        method = req.method
        query = req.query
      }

      if (shouldRenderGraphiQL(request)) {
        res.send(
            renderGraphiQL(dynamicObject {
              subscriptionsEndpoint = "ws://localhost:$port/graphql"
            })
        )
        return@use null
      }

      val graphQLParams = getGraphQLParameters(request)
      processRequest(
          dynamicObject {
            operationName = graphQLParams.operationName
            query = graphQLParams.query
            variables = graphQLParams.variables
            this.request = request
            this.schema = schema
          }
      ).then { result ->
        if (result.type === "RESPONSE") {
          sendResponseResult(result, res)
        } else if (result.type === "MULTIPART_RESPONSE") {
          sendMultipartResponseResult(result, res)
        } else {
          // Should use ws: protocol, not http:
          res.status(422)
        }
      }
    }

    server = app.listen(port) {
      wsServer = Server(dynamicObject {
        this.server = server
        path = "/graphql"
      })

      useServer(dynamicObject {
        this.schema = schema
        this.execute = execute
        this.subscribe = subscribe
      }, wsServer)
    }
  }

  suspend fun url() = url ?: suspendCoroutine { cont ->
    url = "http://localhost:$port/graphql"
    webSocketUrl = "ws://localhost:$port/graphql"
    server.on("listening") { _ ->
      cont.resume(url!!)
    }.unsafeCast<Unit>()
  }

  suspend fun webSocketUrl() = webSocketUrl ?: suspendCoroutine { cont ->
    url = "http://localhost:$port/graphql"
    webSocketUrl = "ws://localhost:$port/graphql"
    server.on("listening") { _ ->
      cont.resume(webSocketUrl!!)
    }.unsafeCast<Unit>()
  }

  suspend fun stop() = suspendCoroutine<Unit> { cont ->
    wsServer.clients.forEach { client ->
      client.close()
    }
    server.close {
      cont.resume(Unit)
    }.unsafeCast<Unit>()
  }
}
