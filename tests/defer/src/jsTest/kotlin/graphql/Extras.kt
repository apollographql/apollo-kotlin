package graphql

import util.jsObject

fun GraphQLSchemaConfig(query: GraphQLObjectType) = jsObject<GraphQLSchemaConfig> { this.query = query }

fun GraphQLObjectTypeConfig(
    name: String,
    fields: () -> dynamic,
) = jsObject<GraphQLObjectTypeConfig> { this.name = name; this.fields = fields }
