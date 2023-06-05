@file:JsModule("graphql")
@file:JsNonModule

package graphql

external class GraphQLSchema(config: GraphQLSchemaConfig)

external interface GraphQLSchemaConfig {
    var query: GraphQLObjectType
    var subscription: GraphQLObjectType?

    // Unused for now
//    var mutation: GraphQLObjectType?
//    var types: Array<GraphQLNamedType>?
    var directives: Array<GraphQLDirective>?
}

external interface GraphQLType

external class GraphQLObjectType(config: GraphQLObjectTypeConfig) : GraphQLType

external interface GraphQLObjectTypeConfig {
    var name: String
    var fields: () -> dynamic
}

external object GraphQLString : GraphQLType
external object GraphQLInt : GraphQLType
external object GraphQLFloat : GraphQLType
external object GraphQLBoolean : GraphQLType
external object GraphQLID : GraphQLType

external class GraphQLList(ofType: GraphQLType) : GraphQLType
external class GraphQLNonNull(ofType: GraphQLType) : GraphQLType

external interface GraphQLDirective

external object GraphQLDeferDirective : GraphQLDirective
external object GraphQLStreamDirective : GraphQLDirective

external val specifiedDirectives: Array<GraphQLDirective>

external val execute: dynamic
external val subscribe: dynamic
