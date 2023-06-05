@file:JsModule("graphql-helix")
@file:JsNonModule

package helix

import kotlin.js.Promise

external fun shouldRenderGraphiQL(request: dynamic): Boolean

external fun renderGraphiQL(options: dynamic): String

external interface GraphQLParams {
  val operationName: String?
  val query: String?
  val variables: dynamic
}

external fun getGraphQLParameters(request: dynamic): GraphQLParams

external interface ProcessRequestResult {
  val type: String
}

external fun processRequest(options: dynamic): Promise<ProcessRequestResult>

external fun sendResponseResult(result: dynamic, rawResponse: dynamic)
external fun sendMultipartResponseResult(result: dynamic, rawResponse: dynamic)
