package com.apollographql.apollo.gradle.test

import com.apollographql.apollo.gradle.internal.RegisterOperations.safelistingHash
import com.google.common.truth.Truth
import org.junit.Test

class RegisterOperationsTest {
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

    Truth.assertThat(query.safelistingHash()).isEqualTo("f9613f4bd179eedf33b9962c22859b3bdf56dfac9c39cefa1ce940894071a482")
  }

  @Test
  fun orderOfFieldsIsIrrelevant() {
    val query = """
      query GetLaunch {
        launch(id: "83", intValue: 42, floatValue: 4.5) {
          cursor
          id
        }
      }
    """.trimIndent()

    Truth.assertThat(query.safelistingHash()).isEqualTo("f9613f4bd179eedf33b9962c22859b3bdf56dfac9c39cefa1ce940894071a482")
  }

  @Test
  fun literalsAreNormalized() {
    val query = """
      query GetLaunch {
        launch(id: "82", intValue: 41, floatValue: 3.5) {
          cursor
          id
        }
      }
    """.trimIndent()

    Truth.assertThat(query.safelistingHash()).isEqualTo("f9613f4bd179eedf33b9962c22859b3bdf56dfac9c39cefa1ce940894071a482")
  }
}