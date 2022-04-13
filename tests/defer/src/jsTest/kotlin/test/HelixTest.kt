package test

import Buffer
import NodeJS.get
import com.apollographql.apollo3.testing.runTest
import graphql.GraphQLObjectType
import graphql.GraphQLObjectTypeConfig
import graphql.GraphQLSchema
import graphql.GraphQLSchemaConfig
import graphql.GraphQLString
import helix.ProcessRequestOptions
import helix.Request
import helix.getGraphQLParameters
import helix.processRequest
import helix.renderGraphiQL
import helix.sendResult
import helix.shouldRenderGraphiQL
import http.createServer
import url.URL
import util.OutgoingHttpHeaders
import util.jsObject
import util.objectFromEntries
import kotlin.test.Test

class HelixTest {
  private suspend fun setUp() {
  }

  private suspend fun tearDown() {
  }

  private val schema = GraphQLSchema(
      GraphQLSchemaConfig(
          query = GraphQLObjectType(
              GraphQLObjectTypeConfig(
                  name = "Query",
                  fields = {
                    jsObject {
                      hello = jsObject {
                        type = GraphQLString
                        resolve = { _: dynamic, _: dynamic -> "Hello, World!" }
                      }
                    }
                  }
              )
          )
      )
  )

  @Test
  fun helixText() = runTest(before = { setUp() }, after = { tearDown() }) {
    println("Hello, World!")

    val server = createServer { req, res ->
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
                  schema = this@HelixTest.schema,
              )
          ).then { result ->
            sendResult(result, res)
          }
        }
      }
    }

    server.listen(4000) {
      println("Listening on http://localhost:4000")
    }
  }
}
