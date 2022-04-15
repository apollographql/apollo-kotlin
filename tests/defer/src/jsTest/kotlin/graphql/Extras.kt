package graphql

import util.dynamicObject

fun GraphQLSchemaConfig(
    query: GraphQLObjectType,
    directives: Array<GraphQLDirective>? = null,
) = dynamicObject<GraphQLSchemaConfig> {
  this.query = query
  directives?.let { this.directives = specifiedDirectives + it }
}

fun GraphQLObjectTypeConfig(
    name: String,
    fields: dynamic,
) = dynamicObject<GraphQLObjectTypeConfig> { this.name = name; this.fields = fields }

fun GraphQLField(type: GraphQLType, args: (() -> dynamic)? = null, resolve: ((dynamic, dynamic) -> dynamic)? = null): dynamic {
  val field = dynamicObject {
    this.type = type
  }
  if (args != null) field.args = args
  if (resolve != null) field.resolve = resolve
  return field
}
