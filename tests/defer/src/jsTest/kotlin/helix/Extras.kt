package helix

import graphql.GraphQLSchema
import util.jsObject

fun Request(body: dynamic, headers: dynamic, method: String, query: dynamic) = jsObject<Request> {
  this.body = body
  this.headers = headers
  this.method = method
  this.query = query
}

fun ProcessRequestOptions(
    operationName: String?,
    query: String?,
    variables: dynamic,
    request: Request,
    schema: GraphQLSchema,
) = jsObject<ProcessRequestOptions> {
  this.operationName = operationName
  this.query = query
  this.variables = variables
  this.request = request
  this.schema = schema
}
