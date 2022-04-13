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

external interface GraphQLType

external class GraphQLObjectType(config: GraphQLObjectTypeConfig) : GraphQLType

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

external object GraphQLString : GraphQLType
external object GraphQLInt : GraphQLType
external object GraphQLFloat : GraphQLType
external object GraphQLBoolean : GraphQLType
external object GraphQLID : GraphQLType

external class GraphQLList(ofType: GraphQLType) : GraphQLType
external class GraphQLNonNull(ofType: GraphQLType) : GraphQLType
