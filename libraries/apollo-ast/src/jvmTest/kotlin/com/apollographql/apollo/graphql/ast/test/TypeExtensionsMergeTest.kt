package com.apollographql.apollo.graphql.ast.test

import com.apollographql.apollo.ast.Issue
import com.apollographql.apollo.ast.MergeOptions
import com.apollographql.apollo.ast.mergeExtensions
import com.apollographql.apollo.ast.parseAsGQLDocument
import com.apollographql.apollo.ast.toGQLDocument
import com.apollographql.apollo.ast.toUtf8
import org.intellij.lang.annotations.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TypeExtensionsMergeTest {

  @Test
  fun cannotChangeFieldType() {
    @Language("graphqls")
    val sdl = """
      type Query {
        random: Int
      }      
      extend type Query {
        random: String
      }
    """.trimIndent()

    sdl.toGQLDocument().mergeExtensions(MergeOptions(true)).issues.apply {
      assertEquals(1, size)
      assertEquals("5:3 Cannot merge field 'random': wrong type 'String' (expected: 'Int')", get(0).pretty())
    }
  }

  @Test
  fun cannotAddDescriptionToType() {
    @Language("graphqls")
    val sdl = """
      type Query {
        random: Int
      }      
      
      "The root query"
      extend type Query
    """.trimIndent()

    // Note how this is a parsing issue, not validation
    sdl.parseAsGQLDocument().issues.apply {
      assertEquals(1, size)
      assertEquals("6:1 Type system extensions cannot have a description", get(0).pretty())
    }
  }

  @Test
  fun cannotAddDescriptionToExistingField() {
    @Language("graphqls")
    val sdl = """
      type Query {
        random: Int
      }      
      
      extend type Query {
        "A new field"
        random: Int
      }
    """.trimIndent()

    sdl.toGQLDocument().mergeExtensions(MergeOptions(true)).issues.apply {
      assertEquals(1, size)
      assertEquals("6:3 Cannot merge field 'random': descriptions cannot be merged", get(0).pretty())
    }
  }

  @Test
  fun cannotAddDescriptionToExistingArgument() {
    @Language("graphqls")
    val sdl = """
      type Query {
        random(id: ID!): Int
      }      
      
      extend type Query {
        random("A new argument" id: ID!): Int
      }
    """.trimIndent()

    sdl.toGQLDocument().mergeExtensions(MergeOptions(true)).issues.apply {
      assertEquals(1, size)
      assertEquals("6:10 Cannot merge argument 'id': descriptions cannot be merged", get(0).pretty())
    }
  }

  @Test
  fun cannotAddDefaultValueToExistingArgument() {
    @Language("graphqls")
    val sdl = """
      type Query {
        random(id: ID!): Int
      }      
      
      extend type Query {
        random(id: ID! = "42"): Int
      }
    """.trimIndent()

    sdl.toGQLDocument().mergeExtensions(MergeOptions(true)).issues.apply {
      assertEquals(1, size)
      assertEquals("6:10 Cannot merge argument 'id': default values cannot be merged", get(0).pretty())
    }
  }

  @Test
  fun canAddDirectiveToExistingField() {
    @Language("graphqls")
    val sdl = """
      type Query {
        random: Int
      }      
      extend type Query {
        random: Int @deprecated
      }
    """.trimIndent()

    sdl.toGQLDocument().mergeExtensions(MergeOptions(true)).getOrThrow().toUtf8().apply {
      assertEquals("""
        type Query {
          random: Int @deprecated
        }
        
      """.trimIndent(), this )
    }
  }

  @Test
  fun canAddArgumentToExistingField() {
    @Language("graphqls")
    val sdl = """
      type Query {
        random: Int
      }      
      extend type Query {
        random(id: ID!): Int
      }
    """.trimIndent()

    sdl.toGQLDocument().mergeExtensions(MergeOptions(true)).getOrThrow().toUtf8().apply {
      assertEquals("""
        type Query {
          random(id: ID!): Int
        }
        
      """.trimIndent(), this )
    }
  }

  private fun Issue.pretty(): String = "${sourceLocation?.line}:${sourceLocation?.column} $message"

  @Test
  fun byDefaultCantExtendField() {
    @Language("graphqls")
    val sdl = """
            type Query {
        random: Int
      }      
      extend type Query {
        random: Int @deprecated
      }
    """.trimIndent()

    sdl.toGQLDocument().mergeExtensions(MergeOptions(false)).issues.apply {
      assertEquals(1, size)
      assertEquals("5:3 There is already a field definition named `random` for this type", first().pretty())
    }
  }
}
