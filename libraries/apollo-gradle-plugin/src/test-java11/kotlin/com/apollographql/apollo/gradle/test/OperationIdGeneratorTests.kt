package com.apollographql.apollo.gradle.test

import com.google.common.truth.Truth
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Test
import util.TestUtils
import util.TestUtils.withTestProject
import util.generatedChild
import util.replaceInText
import java.io.File

class OperationIdGeneratorTests {
  @Test
  fun `operationOutputGenerator is working as expected with classloader isolation`() {
    withTestProject("operationIdsWithIsolation") { dir ->

      var result = TestUtils.executeTask("generateApolloSources", dir)

      val appDir = dir.resolve("app")
      Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":app:generateServiceApolloSources")!!.outcome)

      Truth.assertThat(appDir.generatedChild("service/com/example/GreetingQuery.kt").readText())
          .contains("OPERATION_ID: String = \"GreetingCustomId\"")

      // Change the implementation of the operation ID generator and check again
      File(dir,"apollo-compiler-plugin/src/main/kotlin/apollo/plugin/MyPlugin.kt").replaceInText("CustomId", "anotherCustomId")

      result = TestUtils.executeTask("generateApolloSources", dir)

      Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":app:generateServiceApolloSources")!!.outcome)

      Truth.assertThat(appDir.generatedChild("service/com/example/GreetingQuery.kt").readText())
          .contains("anotherCustomId")
    }
  }
}
