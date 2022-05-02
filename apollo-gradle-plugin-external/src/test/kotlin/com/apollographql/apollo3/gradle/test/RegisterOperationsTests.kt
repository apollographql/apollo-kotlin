package com.apollographql.apollo3.gradle.test

import com.apollographql.apollo3.gradle.internal.RegisterOperations.safelistingHash
import com.apollographql.apollo3.gradle.util.TestUtils
import com.google.common.truth.Truth
import okhttp3.mockwebserver.MockResponse
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.Assert
import org.junit.Test
import java.io.File

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

  @Test
  fun `operation output is parsed correctly`() {
    TestUtils.withTestProject("register-operations") { dir ->
      try {
        TestUtils.executeTask("registerServiceApolloOperations", dir)
      } catch (e: UnexpectedBuildFailure) {
        // because there's no API key, this test will fail but that'll at least verify that we reach that point
        Truth.assertThat(e.message).contains("Cannot push operations")
      }
    }
  }
}