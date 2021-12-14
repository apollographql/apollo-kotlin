package com.apollographql.apollo3.gradle.test

import com.apollographql.apollo3.gradle.internal.RegisterOperations.safelistingHash
import com.google.common.truth.Truth
import org.junit.Test

class RegisterOperationsTests {
  @Test
  fun safelistingHashIsWorking() {
    val query = """
      query GetLaunch {
        launch(id: "83", intValue: 42, floatValue: 4.5) {
          cursor
          id
        }
      }
    """.trimIndent()

    Truth.assertThat(query.safelistingHash()).isEqualTo("33bcef0fa53cb2235e9f069c446d88ee5d425846d58dcb61d30563c9740fccdd")
  }

  @Test
  fun orderOfFieldsIsIrrelevant() {
    val query = """
      query GetLaunch {
        launch(id: "83", intValue: 42, floatValue: 4.5) {
          id
          cursor
        }
      }
    """.trimIndent()

    Truth.assertThat(query.safelistingHash()).isEqualTo("33bcef0fa53cb2235e9f069c446d88ee5d425846d58dcb61d30563c9740fccdd")
  }

  @Test
  fun literalsAreNormalized() {
    val query = """
      query GetLaunch {
        launch(id: "82", intValue: 41, floatValue: 3.5) {
          id
          cursor
        }
      }
    """.trimIndent()

    Truth.assertThat(query.safelistingHash()).isEqualTo("33bcef0fa53cb2235e9f069c446d88ee5d425846d58dcb61d30563c9740fccdd")
  }
}