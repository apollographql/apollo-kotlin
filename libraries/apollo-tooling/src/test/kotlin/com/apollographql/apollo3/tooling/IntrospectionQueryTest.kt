package com.apollographql.apollo3.tooling

import com.apollographql.apollo3.ast.QueryDocumentMinifier
import com.apollographql.apollo3.ast.parseAsGQLDocument
import com.apollographql.apollo3.ast.toUtf8
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Test

class IntrospectionQueryTest {

  // From https://github.com/apollographql/apollo-kotlin/blob/v3.7.4/libraries/apollo-tooling/src/main/kotlin/com/apollographql/apollo3/tooling/SchemaDownloader.kt#L166
  private val expectedIntrospectionQuery = """
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
      it.parseAsGQLDocument(null).valueAssertNoErrors().toUtf8()
    }
  }
  @Test
  fun June2018introspectionQueryDidNotChange() {
    val query = SchemaDownloader.getIntrospectionQuery(SchemaDownloader.SpecVersion.June_2018).normalized()
    val expected = QueryDocumentMinifier.minify(expectedIntrospectionQuery).normalized()

    assertEquals(expected, query)
  }
}
