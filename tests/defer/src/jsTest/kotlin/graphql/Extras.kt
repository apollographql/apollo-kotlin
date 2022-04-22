@file:Suppress("TestFunctionName")

package graphql

import util.dynamicObject

fun GraphQLSchemaConfig(
    query: GraphQLObjectType,
    subscription: GraphQLObjectType? = null,
    directives: Array<GraphQLDirective>? = null,
) = dynamicObject<GraphQLSchemaConfig> {
  this.query = query
  this.subscription = subscription
  directives?.let { this.directives = specifiedDirectives + it }
}

fun GraphQLObjectTypeConfig(
    name: String,
    fields: dynamic,
) = dynamicObject<GraphQLObjectTypeConfig> { this.name = name; this.fields = fields }

fun GraphQLObjectType(
    name: String,
    fields: dynamic,
) = GraphQLObjectType(GraphQLObjectTypeConfig(name, fields))


fun GraphQLField(
    type: GraphQLType,
    args: dynamic = null,
    resolve: ((dynamic, dynamic) -> dynamic)? = null,
    subscribe: ((dynamic, dynamic) -> dynamic)? = null,
): dynamic {
  val field = dynamicObject {
    this.type = type
  }
  if (args != null) field.args = args
  if (resolve != null) field.resolve = resolve
  if (subscribe != null) field.subscribe = subscribe
  return field
}
