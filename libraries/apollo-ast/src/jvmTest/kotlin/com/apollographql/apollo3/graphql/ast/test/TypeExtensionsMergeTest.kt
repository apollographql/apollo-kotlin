package com.apollographql.apollo.graphql.ast.test

import com.apollographql.apollo.ast.GQLObjectTypeDefinition
import com.apollographql.apollo.ast.MergeOptions
import com.apollographql.apollo.ast.mergeExtensions
import com.apollographql.apollo.ast.toMergedGQLDocument
import com.apollographql.apollo.ast.toGQLDocument
import com.apollographql.apollo.ast.toUtf8
import org.intellij.lang.annotations.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TypeExtensionsMergeTest {
  @Test
  fun simpleTest() {
    @Language("graphqls")
    val sdl = """
      type Query {
        random: Int
        list: [String]
        required: Int!
      }
      
      extend type Query {
        random: Int!
        list: [String!]!
        required: Int
        new: Float
      }
    """.trimIndent()

    val result = sdl.toGQLDocument().toMergedGQLDocument(MergeOptions(true))
        .definitions
        .single() as GQLObjectTypeDefinition


    assertEquals("Int!", result.fields.first { it.name == "random" }.type.toUtf8())
    assertEquals("[String!]!", result.fields.first { it.name == "list" }.type.toUtf8())
    assertEquals("Int", result.fields.first { it.name == "required" }.type.toUtf8())
    assertEquals("Float", result.fields.first { it.name == "new" }.type.toUtf8())
  }

  @Test
  fun errors() {
    @Language("graphqls")
    val sdl = """
      type Query {
        random: Int
        list: [String]
        required: Int! 
      }
      
      directive @custom on FIELD_DEFINITION
      
      extend type Query {
        random: String
        list(arg: String): [String!]!
        required: Int @custom
      }
    """.trimIndent()

    val issues = sdl.toGQLDocument().mergeExtensions(MergeOptions(true)).issues

    assertEquals(3, issues.size)
    assertTrue(issues[0].message.contains("its type is not compatible with the original type"))
    assertTrue(issues[1].message.contains("its arguments do not match the arguments of the original field definition"))
    assertTrue(issues[2].message.contains("Cannot add directives to existing field definition"))
  }
}
