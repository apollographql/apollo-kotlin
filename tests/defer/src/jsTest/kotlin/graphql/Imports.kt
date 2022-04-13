@file:JsModule("graphql")
@file:JsNonModule

package graphql

external class GraphQLSchema(config: GraphQLSchemaConfig)

external interface GraphQLSchemaConfig {
    var query: GraphQLObjectType

//    var mutation: GraphQLObjectType?
//    var subscription: GraphQLObjectType?
//    var types: Array<GraphQLNamedType>?
//    var directives: Array<GraphQLDirective>?
}

external class GraphQLObjectType(config: GraphQLObjectTypeConfig)

external interface GraphQLObjectTypeConfig {
    var name: String
    var fields: () -> dynamic
}

external class GraphQLField(config: GraphQLFieldConfig)

external interface GraphQLFieldConfig {
    val type: Any
    val args: () -> Any
    val resolve: (Any, Any) -> Any
}

external object GraphQLString
external object GraphQLInt
external object GraphQLFloat
external object GraphQLBoolean
external object GraphQLID
