package com.apollographql.apollo3.tooling

import com.apollographql.apollo3.ast.parseAsGQLDocument
import com.apollographql.apollo3.ast.toUtf8
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Test

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

  private fun String.asParsedGQLDocument(): String {
    return Buffer().writeUtf8(this).use {
      it.parseAsGQLDocument(null).getOrThrow().toUtf8().trimIndent()
    }
  }

  private fun String.normalized() = this.trimIndent().replace(Regex("\\s+"), " ")

  @Test
  fun canUseTheSameIntrospectionQueryAsVersion3() {
    assertEquals(v3IntrospectionQuery.normalized(), v3IntrospectionQuery.asParsedGQLDocument().normalized())
  }
}
