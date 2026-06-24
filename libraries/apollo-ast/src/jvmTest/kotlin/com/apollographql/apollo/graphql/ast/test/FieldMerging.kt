package com.apollographql.apollo.graphql.ast.test

import com.apollographql.apollo.ast.GQLOperationDefinition
import com.apollographql.apollo.ast.Issue
import com.apollographql.apollo.ast.internal.IssuesScope
import com.apollographql.apollo.ast.internal.fieldsInSetCanMerge
import com.apollographql.apollo.ast.toGQLDocument
import com.apollographql.apollo.ast.toSchema
import kotlin.test.Test

class FieldMerging {
  @Test
  fun invalidField() {
    // language=graphql
    val schema = """
        type Query {
          foo: Int
        }
    """.trimIndent()

    // language=graphql
    val operation = """
      {
         foo
         foo: bar
      }
    """.trimIndent()

    val scope = object : IssuesScope {
      override val issues: MutableList<Issue> = mutableListOf()
    }

    scope.fieldsInSetCanMerge(
        schema = schema.toGQLDocument().toSchema(),
        operation = operation.toGQLDocument().definitions.filterIsInstance<GQLOperationDefinition>().single(),
        fragments = emptyMap()
    )
  }
}