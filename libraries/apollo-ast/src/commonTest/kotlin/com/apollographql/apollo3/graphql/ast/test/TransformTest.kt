package com.apollographql.apollo.graphql.ast.test

import com.apollographql.apollo.ast.GQLField
import com.apollographql.apollo.ast.GQLIntValue
import com.apollographql.apollo.ast.TransformResult
import com.apollographql.apollo.ast.parseAsGQLDocument
import com.apollographql.apollo.ast.toUtf8
import com.apollographql.apollo.ast.transform
import kotlin.test.Test
import kotlin.test.assertEquals

class TransformTest {
  private val query = """
        query GetRepo {
          repository(name: "apollo-kotlin", owner: "apollographql") {
              fieldThatHasBeenThereForever
              newFieldInVersion3 @since(version: 3) @optional
              newFieldInVersion5 @since(version: 5) @optional
          }
        }
    """.trimIndent()

  @Test
  fun stripFieldsWithDirective() {
    val document = query.parseAsGQLDocument().getOrThrow()

    val currentVersion = 4
    val transformed = document.transform {
      if (it is GQLField && (it.minVersion() ?: 0) > currentVersion) {
        TransformResult.Delete
      } else {
        TransformResult.Continue
      }
    }

    val expected = """
      query GetRepo {
        repository(name: "apollo-kotlin", owner: "apollographql") {
          fieldThatHasBeenThereForever
          newFieldInVersion3 @since(version: 3) @optional
        }
      }
      
    """.trimIndent()

    assertEquals(expected, transformed!!.toUtf8())
  }

  private fun GQLField.minVersion(): Int? {
    val directive = directives.firstOrNull { it.name == "since" }

    if (directive == null) {
      return null
    }

    val argument = directive.arguments.firstOrNull()

    check(argument != null && argument.name == "version") {
      "@since requires a single 'version' argument"
    }

    val value = argument.value
    check(value is GQLIntValue) {
      "@since requires an Int argument (is ${argument.value})"
    }

    return value.value.toInt()
  }

  @Test
  fun addField() {
    val query = """
      query TestQuery {
        objectType {
          field
        }
      }
    """.trimIndent()

    val transformed = query.parseAsGQLDocument().getOrThrow().transform {
      if (it is GQLField && it.name == "objectType") {
        val newField = GQLField(
            alias = null,
            name = "newField",
            arguments = emptyList(),
            directives = emptyList(),
            selections = emptyList(),
        )
        TransformResult.Replace(
            it.copy(
                selections = it.selections + newField
            )
        )
      } else {
        TransformResult.Continue
      }
    }

    val expected = """
        |query TestQuery {
        |  objectType {
        |    field
        |    newField
        |  }
        |}
        |
      """.trimMargin()
    val actual = transformed!!.toUtf8()

    assertEquals(expected, actual)
  }
}
