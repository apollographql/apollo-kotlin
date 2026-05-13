package com.apollographql.apollo.tooling

import com.apollographql.apollo.ast.GQLField
import com.apollographql.apollo.ast.GQLFragmentDefinition
import com.apollographql.apollo.ast.GQLOperationDefinition
import com.apollographql.apollo.ast.GQLSelection
import com.apollographql.apollo.ast.parseAsGQLDocument
import com.apollographql.apollo.ast.toUtf8
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.enqueueString
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File
import java.nio.file.Files
import kotlin.test.assertTrue
import kotlin.text.Charsets.UTF_8

class IntrospectionQueryTest {

  // From https://github.com/apollographql/apollo-kotlin/blob/v3.7.4/libraries/apollo-tooling/src/main/kotlin/com/apollographql/apollo/tooling/SchemaDownloader.kt#L166
  private val v3IntrospectionQuery = """
    query IntrospectionQuery {
      __schema {
        queryType { name }
        mutationType { name }
        subscriptionType { name }
        types {
          ...FullType
        }
        directives {
          name
          description
          locations
          args {
            ...InputValue
          }
        }
      }
    }
    fragment FullType on __Type {
      kind
      name
      description
      fields(includeDeprecated: true) {
        name
        description
        args {
          ...InputValue
        }
        type {
          ...TypeRef
        }
        isDeprecated
        deprecationReason
      }
      inputFields {
        ...InputValue
      }
      interfaces {
        ...TypeRef
      }
      enumValues(includeDeprecated: true) {
        name
        description
        isDeprecated
        deprecationReason
      }
      possibleTypes {
        ...TypeRef
      }
    }
    fragment InputValue on __InputValue {
      name
      description
      type { ...TypeRef }
      defaultValue
    }
    fragment TypeRef on __Type {
      kind
      name
      ofType {
        kind
        name
        ofType {
          kind
          name
          ofType {
            kind
            name
            ofType {
              kind
              name
              ofType {
                kind
                name
                ofType {
                  kind
                  name
                  ofType {
                    kind
                    name
                  }
                }
              }
            }
          }
        }
      }
    }""".trimIndent()

  private fun String.normalized(): String {
    return this.parseAsGQLDocument()
        .getOrThrow()
        .let {
          it.copy(
              definitions = it.definitions
                  // Filter out __typename
                  .map {
                    when (it) {
                      is GQLOperationDefinition -> {
                        it.copy(selections = it.selections.withoutTypenames())
                      }

                      is GQLFragmentDefinition -> {
                        it.copy(selections = it.selections.withoutTypenames())
                      }

                      else -> it
                    }
                  }
                  // Sort fragments after operations
                  .sortedWith { a, b ->
                    if (a is GQLFragmentDefinition) {
                      if (b is GQLFragmentDefinition) {
                        a.name.compareTo(b.name)
                      } else {
                        1
                      }
                    } else {
                      if (b is GQLFragmentDefinition) {
                        -1
                      } else {
                        0
                      }
                    }
                  }
          )
              .toUtf8()
        }
  }


  private fun List<GQLSelection>.withoutTypenames(): List<GQLSelection> = mapNotNull {
    if (it is GQLField) {
      if (it.name == "__typename") {
        null
      } else {
        it.copy(selections = it.selections.withoutTypenames())
      }
    } else {
      it
    }
  }


  @Test
  fun canUseTheSameIntrospectionQueryAsVersion3() {
    assertEquals(v3IntrospectionQuery.normalized(), SchemaHelper::class.java.classLoader!!.getResourceAsStream("base-introspection.graphql")!!
        .bufferedReader().readText().normalized()
    )
  }

  @Test
  fun canIntrospectGateway(): Unit = runBlocking {
    MockServer().use { mockServer ->
      // Results of the pre-introspection query
      mockServer.enqueueString(string = """
        {"data": { "__schema":{"types":[{"__typename":"__Type","name":"Query","fields":[{"name":"users","args":[{"name":"where"}]}]},{"__typename":"__Type","name":"User","fields":[{"name":"id","args":[]},{"name":"username","args":[]}]},{"__typename":"__Type","name":"ID","fields":null},{"__typename":"__Type","name":"String","fields":null},{"__typename":"__Type","name":"UserWhere","fields":null},{"__typename":"__Type","name":"Int","fields":null},{"__typename":"__Type","name":"Boolean","fields":null},{"__typename":"__Type","name":"__Schema","fields":[{"name":"description","args":[]},{"name":"types","args":[]},{"name":"queryType","args":[]},{"name":"mutationType","args":[]},{"name":"subscriptionType","args":[]},{"name":"directives","args":[{"name":"includeDeprecated"}]}]},{"__typename":"__Type","name":"__Type","fields":[{"name":"kind","args":[]},{"name":"name","args":[]},{"name":"description","args":[]},{"name":"specifiedByURL","args":[]},{"name":"fields","args":[{"name":"includeDeprecated"}]},{"name":"interfaces","args":[]},{"name":"possibleTypes","args":[]},{"name":"enumValues","args":[{"name":"includeDeprecated"}]},{"name":"inputFields","args":[{"name":"includeDeprecated"}]},{"name":"ofType","args":[]},{"name":"isOneOf","args":[]}]},{"__typename":"__Type","name":"__TypeKind","fields":null},{"__typename":"__Type","name":"__Field","fields":[{"name":"name","args":[]},{"name":"description","args":[]},{"name":"args","args":[{"name":"includeDeprecated"}]},{"name":"type","args":[]},{"name":"isDeprecated","args":[]},{"name":"deprecationReason","args":[]}]},{"__typename":"__Type","name":"__InputValue","fields":[{"name":"name","args":[]},{"name":"description","args":[]},{"name":"type","args":[]},{"name":"defaultValue","args":[]},{"name":"isDeprecated","args":[]},{"name":"deprecationReason","args":[]}]},{"__typename":"__Type","name":"__EnumValue","fields":[{"name":"name","args":[]},{"name":"description","args":[]},{"name":"isDeprecated","args":[]},{"name":"deprecationReason","args":[]}]},{"__typename":"__Type","name":"__Directive","fields":[{"name":"name","args":[]},{"name":"description","args":[]},{"name":"isRepeatable","args":[]},{"name":"locations","args":[]},{"name":"args","args":[{"name":"includeDeprecated"}]},{"name":"isDeprecated","args":[]},{"name":"deprecationReason","args":[]}]},{"__typename":"__Type","name":"__DirectiveLocation","fields":null}]}}}
      """.trimIndent()
      )
      // The gateway doesn't support `__Type.isOneOf` despite advertising it
      mockServer.enqueueString(statusCode = 500, string = """
        {"errors":[{"message":"Cannot query field \"isOneOf\" on type \"__Type\".","extensions":{"code":"INVALID_GRAPHQL","stacktrace":["GraphQLError: Cannot query field \"isOneOf\" on type \"__Type\".","    at Object.err (/Users/martinbonnin/Downloads/oneof/node_modules/@apollo/federation-internals/dist/error.js:11:32)","    at validate (/Users/martinbonnin/Downloads/oneof/node_modules/@apollo/federation-internals/dist/operations.js:15:46)","    at selectionOfNode (/Users/martinbonnin/Downloads/oneof/node_modules/@apollo/federation-internals/dist/operations.js:2387:13)","    at selectionSetOfNode (/Users/martinbonnin/Downloads/oneof/node_modules/@apollo/federation-internals/dist/operations.js:2367:24)","    at /Users/martinbonnin/Downloads/oneof/node_modules/@apollo/federation-internals/dist/operations.js:2444:42","    at Array.forEach (<anonymous>)","    at operationFromDocument (/Users/martinbonnin/Downloads/oneof/node_modules/@apollo/federation-internals/dist/operations.js:2440:26)","    at /Users/martinbonnin/Downloads/oneof/node_modules/@apollo/gateway/dist/index.js:78:100","    at NoopContextManager.with (/Users/martinbonnin/Downloads/oneof/node_modules/@opentelemetry/api/build/src/context/NoopContextManager.js:14:19)","    at ContextAPI.with (/Users/martinbonnin/Downloads/oneof/node_modules/@opentelemetry/api/build/src/api/context.js:51:46)"]}}]}
      """.trimIndent()
      )
      // The gateway doesn't support `__Directive.isDeprecated` despite advertising it
      mockServer.enqueueString(statusCode = 500, string = """
        {"errors":[{"message":"Cannot query field \"isDeprecated\" on type \"__Directive\".","extensions":{"code":"INVALID_GRAPHQL","stacktrace":["GraphQLError: Cannot query field \"isDeprecated\" on type \"__Directive\".","    at Object.err (/Users/martinbonnin/Downloads/oneof/node_modules/@apollo/federation-internals/dist/error.js:11:32)","    at validate (/Users/martinbonnin/Downloads/oneof/node_modules/@apollo/federation-internals/dist/operations.js:15:46)","    at selectionOfNode (/Users/martinbonnin/Downloads/oneof/node_modules/@apollo/federation-internals/dist/operations.js:2387:13)","    at selectionSetOfNode (/Users/martinbonnin/Downloads/oneof/node_modules/@apollo/federation-internals/dist/operations.js:2367:24)","    at selectionOfNode (/Users/martinbonnin/Downloads/oneof/node_modules/@apollo/federation-internals/dist/operations.js:2390:19)","    at selectionSetOfNode (/Users/martinbonnin/Downloads/oneof/node_modules/@apollo/federation-internals/dist/operations.js:2367:24)","    at selectionOfNode (/Users/martinbonnin/Downloads/oneof/node_modules/@apollo/federation-internals/dist/operations.js:2390:19)","    at selectionSetOfNode (/Users/martinbonnin/Downloads/oneof/node_modules/@apollo/federation-internals/dist/operations.js:2363:43)","    at parseSelectionSet (/Users/martinbonnin/Downloads/oneof/node_modules/@apollo/federation-internals/dist/operations.js:2473:26)","    at operationFromAST (/Users/martinbonnin/Downloads/oneof/node_modules/@apollo/federation-internals/dist/operations.js:2457:55)"]}}]}
      """.trimIndent()
      )
      // Actual introspection
      mockServer.enqueueString("""
        {"data":{"__schema":{"queryType":{"name":"Query"},"mutationType":null,"subscriptionType":null,"types":[{"kind":"OBJECT","name":"Query","description":null,"fields":[{"name":"users","description":null,"args":[{"name":"where","description":null,"type":{"kind":"NON_NULL","name":null,"ofType":{"kind":"INPUT_OBJECT","name":"UserWhere","ofType":null}},"defaultValue":null,"isDeprecated":false,"deprecationReason":null}],"type":{"kind":"NON_NULL","name":null,"ofType":{"kind":"LIST","name":null,"ofType":{"kind":"NON_NULL","name":null,"ofType":{"kind":"OBJECT","name":"User","ofType":null}}}},"isDeprecated":false,"deprecationReason":null}],"inputFields":null,"interfaces":[],"enumValues":null,"possibleTypes":null,"specifiedByURL":null},{"kind":"OBJECT","name":"User","description":null,"fields":[{"name":"id","description":null,"args":[],"type":{"kind":"NON_NULL","name":null,"ofType":{"kind":"SCALAR","name":"ID","ofType":null}},"isDeprecated":false,"deprecationReason":null},{"name":"username","description":null,"args":[],"type":{"kind":"SCALAR","name":"String","ofType":null},"isDeprecated":false,"deprecationReason":null}],"inputFields":null,"interfaces":[],"enumValues":null,"possibleTypes":null,"specifiedByURL":null},{"kind":"SCALAR","name":"ID","description":"The `ID` scalar type represents a unique identifier, often used to refetch an object or as key for a cache. The ID type appears in a JSON response as a String; however, it is not intended to be human-readable. When expected as an input type, any string (such as `\"4\"`) or integer (such as `4`) input value will be accepted as an ID.","fields":null,"inputFields":null,"interfaces":null,"enumValues":null,"possibleTypes":null,"specifiedByURL":null},{"kind":"SCALAR","name":"String","description":"The `String` scalar type represents textual data, represented as UTF-8 character sequences. The String type is most often used by GraphQL to represent free-form human-readable text.","fields":null,"inputFields":null,"interfaces":null,"enumValues":null,"possibleTypes":null,"specifiedByURL":null},{"kind":"INPUT_OBJECT","name":"UserWhere","description":null,"fields":null,"inputFields":[{"name":"foo","description":null,"type":{"kind":"SCALAR","name":"Int","ofType":null},"defaultValue":null,"isDeprecated":false,"deprecationReason":null},{"name":"bar","description":null,"type":{"kind":"SCALAR","name":"Int","ofType":null},"defaultValue":null,"isDeprecated":false,"deprecationReason":null}],"interfaces":null,"enumValues":null,"possibleTypes":null,"specifiedByURL":null},{"kind":"SCALAR","name":"Int","description":"The `Int` scalar type represents non-fractional signed whole numeric values. Int can represent values between -(2^31) and 2^31 - 1.","fields":null,"inputFields":null,"interfaces":null,"enumValues":null,"possibleTypes":null,"specifiedByURL":null},{"kind":"SCALAR","name":"Boolean","description":"The `Boolean` scalar type represents `true` or `false`.","fields":null,"inputFields":null,"interfaces":null,"enumValues":null,"possibleTypes":null,"specifiedByURL":null},{"kind":"OBJECT","name":"__Schema","description":"A GraphQL Schema defines the capabilities of a GraphQL server. It exposes all available types and directives on the server, as well as the entry points for query, mutation, and subscription operations.","fields":[{"name":"description","description":null,"args":[],"type":{"kind":"SCALAR","name":"String","ofType":null},"isDeprecated":false,"deprecationReason":null},{"name":"types","description":"A list of all types supported by this server.","args":[],"type":{"kind":"NON_NULL","name":null,"ofType":{"kind":"LIST","name":null,"ofType":{"kind":"NON_NULL","name":null,"ofType":{"kind":"OBJECT","name":"__Type","ofType":null}}}},"isDeprecated":false,"deprecationReason":null},{"name":"queryType","description":"The type that query operations will be rooted at.","args":[],"type":{"kind":"NON_NULL","name":null,"ofType":{"kind":"OBJECT","name":"__Type","ofType":null}},"isDeprecated":false,"deprecationReason":null},{"name":"mutationType","description":"If this server supports mutation, the type that mutation operations will be rooted at.","args":[],"type":{"kind":"OBJECT","name":"__Type","ofType":null},"isDeprecated":false,"deprecationReason":null},{"name":"subscriptionType","description":"If this server support subscription, the type that subscription operations will be rooted at.","args":[],"type":{"kind":"OBJECT","name":"__Type","ofType":null},"isDeprecated":false,"deprecationReason":null},{"name":"directives","description":"A list of all directives supported by this server.","args":[{"name":"includeDeprecated","description":null,"type":{"kind":"NON_NULL","name":null,"ofType":{"kind":"SCALAR","name":"Boolean","ofType":null}},"defaultValue":"false","isDeprecated":false,"deprecationReason":null}],"type":{"kind":"NON_NULL","name":null,"ofType":{"kind":"LIST","name":null,"ofType":{"kind":"NON_NULL","name":null,"ofType":{"kind":"OBJECT","name":"__Directive","ofType":null}}}},"isDeprecated":false,"deprecationReason":null}],"inputFields":null,"interfaces":[],"enumValues":null,"possibleTypes":null,"specifiedByURL":null},{"kind":"OBJECT","name":"__Type","description":"The fundamental unit of any GraphQL Schema is the type. There are many kinds of types in GraphQL as represented by the `__TypeKind` enum.\n\nDepending on the kind of a type, certain fields describe information about that type. Scalar types provide no information beyond a name, description and optional `specifiedByURL`, while Enum types provide their values. Object and Interface types provide the fields they describe. Abstract types, Union and Interface, provide the Object types possible at runtime. List and NonNull types compose other types.","fields":[{"name":"kind","description":null,"args":[],"type":{"kind":"NON_NULL","name":null,"ofType":{"kind":"ENUM","name":"__TypeKind","ofType":null}},"isDeprecated":false,"deprecationReason":null},{"name":"name","description":null,"args":[],"type":{"kind":"SCALAR","name":"String","ofType":null},"isDeprecated":false,"deprecationReason":null},{"name":"description","description":null,"args":[],"type":{"kind":"SCALAR","name":"String","ofType":null},"isDeprecated":false,"deprecationReason":null},{"name":"specifiedByURL","description":null,"args":[],"type":{"kind":"SCALAR","name":"String","ofType":null},"isDeprecated":false,"deprecationReason":null},{"name":"fields","description":null,"args":[{"name":"includeDeprecated","description":null,"type":{"kind":"SCALAR","name":"Boolean","ofType":null},"defaultValue":"false","isDeprecated":false,"deprecationReason":null}],"type":{"kind":"LIST","name":null,"ofType":{"kind":"NON_NULL","name":null,"ofType":{"kind":"OBJECT","name":"__Field","ofType":null}}},"isDeprecated":false,"deprecationReason":null},{"name":"interfaces","description":null,"args":[],"type":{"kind":"LIST","name":null,"ofType":{"kind":"NON_NULL","name":null,"ofType":{"kind":"OBJECT","name":"__Type","ofType":null}}},"isDeprecated":false,"deprecationReason":null},{"name":"possibleTypes","description":null,"args":[],"type":{"kind":"LIST","name":null,"ofType":{"kind":"NON_NULL","name":null,"ofType":{"kind":"OBJECT","name":"__Type","ofType":null}}},"isDeprecated":false,"deprecationReason":null},{"name":"enumValues","description":null,"args":[{"name":"includeDeprecated","description":null,"type":{"kind":"SCALAR","name":"Boolean","ofType":null},"defaultValue":"false","isDeprecated":false,"deprecationReason":null}],"type":{"kind":"LIST","name":null,"ofType":{"kind":"NON_NULL","name":null,"ofType":{"kind":"OBJECT","name":"__EnumValue","ofType":null}}},"isDeprecated":false,"deprecationReason":null},{"name":"inputFields","description":null,"args":[{"name":"includeDeprecated","description":null,"type":{"kind":"SCALAR","name":"Boolean","ofType":null},"defaultValue":"false","isDeprecated":false,"deprecationReason":null}],"type":{"kind":"LIST","name":null,"ofType":{"kind":"NON_NULL","name":null,"ofType":{"kind":"OBJECT","name":"__InputValue","ofType":null}}},"isDeprecated":false,"deprecationReason":null},{"name":"ofType","description":null,"args":[],"type":{"kind":"OBJECT","name":"__Type","ofType":null},"isDeprecated":false,"deprecationReason":null},{"name":"isOneOf","description":null,"args":[],"type":{"kind":"SCALAR","name":"Boolean","ofType":null},"isDeprecated":false,"deprecationReason":null}],"inputFields":null,"interfaces":[],"enumValues":null,"possibleTypes":null,"specifiedByURL":null},{"kind":"ENUM","name":"__TypeKind","description":"An enum describing what kind of type a given `__Type` is.","fields":null,"inputFields":null,"interfaces":null,"enumValues":[{"name":"SCALAR","description":"Indicates this type is a scalar.","isDeprecated":false,"deprecationReason":null},{"name":"OBJECT","description":"Indicates this type is an object. `fields` and `interfaces` are valid fields.","isDeprecated":false,"deprecationReason":null},{"name":"INTERFACE","description":"Indicates this type is an interface. `fields`, `interfaces`, and `possibleTypes` are valid fields.","isDeprecated":false,"deprecationReason":null},{"name":"UNION","description":"Indicates this type is a union. `possibleTypes` is a valid field.","isDeprecated":false,"deprecationReason":null},{"name":"ENUM","description":"Indicates this type is an enum. `enumValues` is a valid field.","isDeprecated":false,"deprecationReason":null},{"name":"INPUT_OBJECT","description":"Indicates this type is an input object. `inputFields` is a valid field.","isDeprecated":false,"deprecationReason":null},{"name":"LIST","description":"Indicates this type is a list. `ofType` is a valid field.","isDeprecated":false,"deprecationReason":null},{"name":"NON_NULL","description":"Indicates this type is a non-null. `ofType` is a valid field.","isDeprecated":false,"deprecationReason":null}],"possibleTypes":null,"specifiedByURL":null},{"kind":"OBJECT","name":"__Field","description":"Object and Interface types are described by a list of Fields, each of which has a name, potentially a list of arguments, and a return type.","fields":[{"name":"name","description":null,"args":[],"type":{"kind":"NON_NULL","name":null,"ofType":{"kind":"SCALAR","name":"String","ofType":null}},"isDeprecated":false,"deprecationReason":null},{"name":"description","description":null,"args":[],"type":{"kind":"SCALAR","name":"String","ofType":null},"isDeprecated":false,"deprecationReason":null},{"name":"args","description":null,"args":[{"name":"includeDeprecated","description":null,"type":{"kind":"SCALAR","name":"Boolean","ofType":null},"defaultValue":"false","isDeprecated":false,"deprecationReason":null}],"type":{"kind":"NON_NULL","name":null,"ofType":{"kind":"LIST","name":null,"ofType":{"kind":"NON_NULL","name":null,"ofType":{"kind":"OBJECT","name":"__InputValue","ofType":null}}}},"isDeprecated":false,"deprecationReason":null},{"name":"type","description":null,"args":[],"type":{"kind":"NON_NULL","name":null,"ofType":{"kind":"OBJECT","name":"__Type","ofType":null}},"isDeprecated":false,"deprecationReason":null},{"name":"isDeprecated","description":null,"args":[],"type":{"kind":"NON_NULL","name":null,"ofType":{"kind":"SCALAR","name":"Boolean","ofType":null}},"isDeprecated":false,"deprecationReason":null},{"name":"deprecationReason","description":null,"args":[],"type":{"kind":"SCALAR","name":"String","ofType":null},"isDeprecated":false,"deprecationReason":null}],"inputFields":null,"interfaces":[],"enumValues":null,"possibleTypes":null,"specifiedByURL":null},{"kind":"OBJECT","name":"__InputValue","description":"Arguments provided to Fields or Directives and the input fields of an InputObject are represented as Input Values which describe their type and optionally a default value.","fields":[{"name":"name","description":null,"args":[],"type":{"kind":"NON_NULL","name":null,"ofType":{"kind":"SCALAR","name":"String","ofType":null}},"isDeprecated":false,"deprecationReason":null},{"name":"description","description":null,"args":[],"type":{"kind":"SCALAR","name":"String","ofType":null},"isDeprecated":false,"deprecationReason":null},{"name":"type","description":null,"args":[],"type":{"kind":"NON_NULL","name":null,"ofType":{"kind":"OBJECT","name":"__Type","ofType":null}},"isDeprecated":false,"deprecationReason":null},{"name":"defaultValue","description":"A GraphQL-formatted string representing the default value for this input value.","args":[],"type":{"kind":"SCALAR","name":"String","ofType":null},"isDeprecated":false,"deprecationReason":null},{"name":"isDeprecated","description":null,"args":[],"type":{"kind":"NON_NULL","name":null,"ofType":{"kind":"SCALAR","name":"Boolean","ofType":null}},"isDeprecated":false,"deprecationReason":null},{"name":"deprecationReason","description":null,"args":[],"type":{"kind":"SCALAR","name":"String","ofType":null},"isDeprecated":false,"deprecationReason":null}],"inputFields":null,"interfaces":[],"enumValues":null,"possibleTypes":null,"specifiedByURL":null},{"kind":"OBJECT","name":"__EnumValue","description":"One possible value for a given Enum. Enum values are unique values, not a placeholder for a string or numeric value. However an Enum value is returned in a JSON response as a string.","fields":[{"name":"name","description":null,"args":[],"type":{"kind":"NON_NULL","name":null,"ofType":{"kind":"SCALAR","name":"String","ofType":null}},"isDeprecated":false,"deprecationReason":null},{"name":"description","description":null,"args":[],"type":{"kind":"SCALAR","name":"String","ofType":null},"isDeprecated":false,"deprecationReason":null},{"name":"isDeprecated","description":null,"args":[],"type":{"kind":"NON_NULL","name":null,"ofType":{"kind":"SCALAR","name":"Boolean","ofType":null}},"isDeprecated":false,"deprecationReason":null},{"name":"deprecationReason","description":null,"args":[],"type":{"kind":"SCALAR","name":"String","ofType":null},"isDeprecated":false,"deprecationReason":null}],"inputFields":null,"interfaces":[],"enumValues":null,"possibleTypes":null,"specifiedByURL":null},{"kind":"OBJECT","name":"__Directive","description":"A Directive provides a way to describe alternate runtime execution and type validation behavior in a GraphQL document.\n\nIn some cases, you need to provide options to alter GraphQL's execution behavior in ways field arguments will not suffice, such as conditionally including or skipping a field. Directives provide this by describing additional information to the executor.","fields":[{"name":"name","description":null,"args":[],"type":{"kind":"NON_NULL","name":null,"ofType":{"kind":"SCALAR","name":"String","ofType":null}},"isDeprecated":false,"deprecationReason":null},{"name":"description","description":null,"args":[],"type":{"kind":"SCALAR","name":"String","ofType":null},"isDeprecated":false,"deprecationReason":null},{"name":"isRepeatable","description":null,"args":[],"type":{"kind":"NON_NULL","name":null,"ofType":{"kind":"SCALAR","name":"Boolean","ofType":null}},"isDeprecated":false,"deprecationReason":null},{"name":"locations","description":null,"args":[],"type":{"kind":"NON_NULL","name":null,"ofType":{"kind":"LIST","name":null,"ofType":{"kind":"NON_NULL","name":null,"ofType":{"kind":"ENUM","name":"__DirectiveLocation","ofType":null}}}},"isDeprecated":false,"deprecationReason":null},{"name":"args","description":null,"args":[{"name":"includeDeprecated","description":null,"type":{"kind":"SCALAR","name":"Boolean","ofType":null},"defaultValue":"false","isDeprecated":false,"deprecationReason":null}],"type":{"kind":"NON_NULL","name":null,"ofType":{"kind":"LIST","name":null,"ofType":{"kind":"NON_NULL","name":null,"ofType":{"kind":"OBJECT","name":"__InputValue","ofType":null}}}},"isDeprecated":false,"deprecationReason":null},{"name":"isDeprecated","description":null,"args":[],"type":{"kind":"NON_NULL","name":null,"ofType":{"kind":"SCALAR","name":"Boolean","ofType":null}},"isDeprecated":false,"deprecationReason":null},{"name":"deprecationReason","description":null,"args":[],"type":{"kind":"SCALAR","name":"String","ofType":null},"isDeprecated":false,"deprecationReason":null}],"inputFields":null,"interfaces":[],"enumValues":null,"possibleTypes":null,"specifiedByURL":null},{"kind":"ENUM","name":"__DirectiveLocation","description":"A Directive can be adjacent to many parts of the GraphQL language, a __DirectiveLocation describes one such possible adjacencies.","fields":null,"inputFields":null,"interfaces":null,"enumValues":[{"name":"QUERY","description":"Location adjacent to a query operation.","isDeprecated":false,"deprecationReason":null},{"name":"MUTATION","description":"Location adjacent to a mutation operation.","isDeprecated":false,"deprecationReason":null},{"name":"SUBSCRIPTION","description":"Location adjacent to a subscription operation.","isDeprecated":false,"deprecationReason":null},{"name":"FIELD","description":"Location adjacent to a field.","isDeprecated":false,"deprecationReason":null},{"name":"FRAGMENT_DEFINITION","description":"Location adjacent to a fragment definition.","isDeprecated":false,"deprecationReason":null},{"name":"FRAGMENT_SPREAD","description":"Location adjacent to a fragment spread.","isDeprecated":false,"deprecationReason":null},{"name":"INLINE_FRAGMENT","description":"Location adjacent to an inline fragment.","isDeprecated":false,"deprecationReason":null},{"name":"VARIABLE_DEFINITION","description":"Location adjacent to a variable definition.","isDeprecated":false,"deprecationReason":null},{"name":"SCHEMA","description":"Location adjacent to a schema definition.","isDeprecated":false,"deprecationReason":null},{"name":"SCALAR","description":"Location adjacent to a scalar definition.","isDeprecated":false,"deprecationReason":null},{"name":"OBJECT","description":"Location adjacent to an object type definition.","isDeprecated":false,"deprecationReason":null},{"name":"FIELD_DEFINITION","description":"Location adjacent to a field definition.","isDeprecated":false,"deprecationReason":null},{"name":"ARGUMENT_DEFINITION","description":"Location adjacent to an argument definition.","isDeprecated":false,"deprecationReason":null},{"name":"INTERFACE","description":"Location adjacent to an interface definition.","isDeprecated":false,"deprecationReason":null},{"name":"UNION","description":"Location adjacent to a union definition.","isDeprecated":false,"deprecationReason":null},{"name":"ENUM","description":"Location adjacent to an enum definition.","isDeprecated":false,"deprecationReason":null},{"name":"ENUM_VALUE","description":"Location adjacent to an enum value definition.","isDeprecated":false,"deprecationReason":null},{"name":"INPUT_OBJECT","description":"Location adjacent to an input object type definition.","isDeprecated":false,"deprecationReason":null},{"name":"INPUT_FIELD_DEFINITION","description":"Location adjacent to an input object field definition.","isDeprecated":false,"deprecationReason":null},{"name":"DIRECTIVE_DEFINITION","description":"Location adjacent to a directive definition.","isDeprecated":false,"deprecationReason":null}],"possibleTypes":null,"specifiedByURL":null}],"directives":[{"name":"include","description":"Directs the executor to include this field or fragment only when the `if` argument is true.","locations":["FIELD","FRAGMENT_SPREAD","INLINE_FRAGMENT"],"args":[{"name":"if","description":"Included when true.","type":{"kind":"NON_NULL","name":null,"ofType":{"kind":"SCALAR","name":"Boolean","ofType":null}},"defaultValue":null,"isDeprecated":false,"deprecationReason":null}],"isRepeatable":false},{"name":"skip","description":"Directs the executor to skip this field or fragment when the `if` argument is true.","locations":["FIELD","FRAGMENT_SPREAD","INLINE_FRAGMENT"],"args":[{"name":"if","description":"Skipped when true.","type":{"kind":"NON_NULL","name":null,"ofType":{"kind":"SCALAR","name":"Boolean","ofType":null}},"defaultValue":null,"isDeprecated":false,"deprecationReason":null}],"isRepeatable":false},{"name":"deprecated","description":"Marks an element of a GraphQL schema as no longer supported.","locations":["FIELD_DEFINITION","ARGUMENT_DEFINITION","INPUT_FIELD_DEFINITION","ENUM_VALUE","DIRECTIVE_DEFINITION"],"args":[{"name":"reason","description":"Explains why this element was deprecated, usually also including a suggestion for how to access supported similar data. Formatted using the Markdown syntax, as specified by [CommonMark](https://commonmark.org/).","type":{"kind":"SCALAR","name":"String","ofType":null},"defaultValue":"\"No longer supported\"","isDeprecated":false,"deprecationReason":null}],"isRepeatable":false},{"name":"specifiedBy","description":"Exposes a URL that specifies the behavior of this scalar.","locations":["SCALAR"],"args":[{"name":"url","description":"The URL that specifies the behavior of this scalar.","type":{"kind":"NON_NULL","name":null,"ofType":{"kind":"SCALAR","name":"String","ofType":null}},"defaultValue":null,"isDeprecated":false,"deprecationReason":null}],"isRepeatable":false},{"name":"oneOf","description":"Indicates exactly one field must be supplied and this field must not be `null`.","locations":["INPUT_OBJECT"],"args":[],"isRepeatable":false}],"description":null}}}
      """.trimIndent()
      )
      SchemaDownloader.download(
          mockServer.url(),
          graph = null,
          key = null,
          graphVariant = "",
          schema = Files.createTempFile("apollo-", "-tests").toFile(),
          insecure = false,
          headers = emptyMap(),
      )

      mockServer.takeRequest() // pre-introspection
      mockServer.takeRequest().body.string(UTF_8).apply {
        assertTrue(contains("isOneOf"))
        assertTrue(contains("directives(includeDeprecated: true)"))
      }
      mockServer.takeRequest().body.string(UTF_8).apply {
        assertFalse(contains("isOneOf"))
        assertTrue(contains("directives(includeDeprecated: true)"))
      }
      mockServer.takeRequest().body.string(UTF_8).apply {
        assertFalse(contains("isOneOf"))
        assertFalse(contains("directives(includeDeprecated: true)"))
      }
    }
  }
}
