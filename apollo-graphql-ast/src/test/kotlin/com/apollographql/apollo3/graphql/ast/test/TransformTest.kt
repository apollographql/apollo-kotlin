package com.apollographql.apollo3.ast.test

import com.apollographql.apollo3.ast.GQLField
import com.apollographql.apollo3.ast.GQLIntValue
import com.apollographql.apollo3.ast.parseAsGQLDocument
import com.apollographql.apollo3.ast.toUtf8
import com.apollographql.apollo3.ast.transform
import org.junit.Test
import kotlin.test.assertEquals

class TransformTest {
  private val query = """
        query GetRepo {
          repository(name: "apollo-android", owner: "apollographql") {
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
        null
      } else {
        it
      }
    }

    val expected = """
      query GetRepo {
        repository(name: "apollo-android", owner: "apollographql") {
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

    val argument = directive.arguments?.arguments?.first()

    check(argument != null && argument.name == "version") {
      "@since requires a single 'version' argument"
    }

    val value = argument.value
    check(value is GQLIntValue) {
      "@since requires an Int argument (is ${argument.value})"
    }

    return value.value
  }

}