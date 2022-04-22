@file:JsModule("graphql-helix")
@file:JsNonModule

package helix

import graphql.GraphQLSchema
import kotlin.js.Promise

external interface Request {
  var body: dynamic
  var headers: dynamic
  var method: String
  var query: dynamic
}

external fun shouldRenderGraphiQL(request: Request): Boolean

external fun renderGraphiQL(): String

external interface GraphQLParams {
  val operationName: String?
  val query: String?
  val variables: dynamic
}

external fun getGraphQLParameters(request: Request): GraphQLParams

external interface ProcessRequestOptions {
  var operationName: String?
  var query: String?
  var variables: dynamic
  var request: Request
  var schema: GraphQLSchema
}

external interface ProcessRequestResult

external fun processRequest(options: ProcessRequestOptions): Promise<ProcessRequestResult>

external fun sendResult(result: ProcessRequestResult, rawResponse: http.ServerResponse): Promise<Unit>
