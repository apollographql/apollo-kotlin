package graphql

import util.dynamicObject

fun GraphQLSchemaConfig(query: GraphQLObjectType) = dynamicObject<GraphQLSchemaConfig> { this.query = query }

fun GraphQLObjectTypeConfig(
    name: String,
    fields: () -> dynamic,
) = dynamicObject<GraphQLObjectTypeConfig> { this.name = name; this.fields = fields }
