package helix

import Buffer
import NodeJS.get
import graphql.GraphQLSchema
import http.createServer
import net.AddressInfo
import url.URL
import util.OutgoingHttpHeaders
import util.objectFromEntries
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class HelixServer
(
    private val schema: GraphQLSchema,
    port: Long? = null,
) {
  private var url: String? = null

  private val server = createServer { req, res ->
    val url = URL(req.url, "http://${req.headers["Host"]}")

    if (url.pathname != "/graphql") {
      res.writeHead(404)
      res.end("Not found")
      return@createServer
    }

    val payload = StringBuilder()

    req.on("data") { chunk ->
      when (chunk) {
        is String -> payload.append(chunk)
        is Buffer -> payload.append(chunk.toString("utf8"))
      }
    }

    req.on("end") { _ ->
      val request = Request(
          body = JSON.parse<Any>(payload.toString().ifBlank { "{}" }),
          headers = req.headers,
          method = req.method,
          query = objectFromEntries(url.searchParams),
      )

      if (shouldRenderGraphiQL(request)) {
        res.writeHead(200, OutgoingHttpHeaders("content-type" to "text/html"))
        res.end(renderGraphiQL())
      } else {
        val graphQLParams = getGraphQLParameters(request)
        processRequest(
            ProcessRequestOptions(
                operationName = graphQLParams.operationName,
                query = graphQLParams.query,
                variables = graphQLParams.variables,
                request = request,
                schema = schema,
            )
        ).then { result ->
          sendResult(result, res)
        }
      }
    }
  }.apply {
    if (port != null) {
      listen(port)
    } else {
      listen()
    }
  }

  suspend fun url() = url ?: suspendCoroutine { cont ->
    url = "http://localhost:${server.address().unsafeCast<AddressInfo>().port}/graphql"
    server.on("listening") { _ ->
      cont.resume(url!!)
    }
  }

  suspend fun stop() = suspendCoroutine<Unit> { cont ->
    server.close {
      cont.resume(Unit)
    }
  }
}
