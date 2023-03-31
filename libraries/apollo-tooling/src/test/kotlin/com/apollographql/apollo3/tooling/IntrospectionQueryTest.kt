package com.apollographql.apollo3.tooling

import com.apollographql.apollo3.ast.GQLField
import com.apollographql.apollo3.ast.GQLFragmentDefinition
import com.apollographql.apollo3.ast.GQLOperationDefinition
import com.apollographql.apollo3.ast.GQLSelectionSet
import com.apollographql.apollo3.ast.parseAsGQLDocument
import com.apollographql.apollo3.ast.toUtf8
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Test
import com.apollographql.apollo3.tooling.graphql.june2018.IntrospectionQuery as GraphQLJune2018IntrospectionQuery

class IntrospectionQueryTest {

  // From https://github.com/apollographql/apollo-kotlin/blob/v3.7.4/libraries/apollo-tooling/src/main/kotlin/com/apollographql/apollo3/tooling/SchemaDownloader.kt#L166
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
    return Buffer().writeUtf8(this).use {
      it.parseAsGQLDocument(null).getOrThrow()
          .let {
            it.copy(
                definitions = it.definitions
                    // Filter out __typename
                    .map {
                      when (it) {
                        is GQLOperationDefinition -> {
                          it.copy(selectionSet = it.selectionSet.withoutTypenames())
                        }

                        is GQLFragmentDefinition -> {
                          it.copy(selectionSet = it.selectionSet.withoutTypenames())
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
  }

  private fun GQLSelectionSet.withoutTypenames(): GQLSelectionSet = copy(
      selections = selections.mapNotNull {
        if (it is GQLField) {
          if (it.name == "__typename") {
            null
          } else {
            it.copy(selectionSet = it.selectionSet?.withoutTypenames())
          }
        } else {
          it
        }
      }
  )

  @Test
  fun canUseTheSameIntrospectionQueryAsVersion3() {
    assertEquals(v3IntrospectionQuery.normalized(), GraphQLJune2018IntrospectionQuery.OPERATION_DOCUMENT.normalized())
  }
}
