package com.apollographql.apollo.gradle.test

import com.apollographql.apollo.gradle.util.TestUtils
import com.apollographql.apollo.gradle.util.TestUtils.withSimpleProject
import com.apollographql.apollo.gradle.util.replaceInText
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Test
import java.io.File

class CustomIdGeneratorTests {
  @Test
  fun `up-to-date checks are working`() {
    val apolloConfiguration = """
      class MyIdGenerator implements CustomIdGenerator {
          String apply(String queryString) {
              return queryString.length().toString();
          }
      }
      
      apollo {
        customIdGenerator = new MyIdGenerator()
      }
    """.trimIndent()

    withSimpleProject(apolloConfiguration = apolloConfiguration) {dir ->
      val gradleFile = File(dir, "build.gradle").readText()

      File(dir, "build.gradle").writeText("import com.apollographql.apollo.compiler.CustomIdGenerator\n$gradleFile")

      var result = TestUtils.executeTask("generateApolloSources", dir)

      Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":generateApolloSources")!!.outcome)

      result = TestUtils.executeTask("generateApolloSources", dir)

      Assert.assertEquals(TaskOutcome.UP_TO_DATE, result.task(":generateApolloSources")!!.outcome)
    }
  }

  @Test
  fun `changing the customIdGenerator recompiles sources`() {
    val apolloConfiguration = """
      class MyIdGenerator implements CustomIdGenerator {
          String apply(String queryString) {
              return queryString.length().toString();
          }
      }
      
      apollo {
        customIdGenerator = new MyIdGenerator()
      }
    """.trimIndent()

    withSimpleProject(apolloConfiguration = apolloConfiguration) {dir ->
      val gradleFile = File(dir, "build.gradle").readText()

      File(dir, "build.gradle").writeText("import com.apollographql.apollo.compiler.CustomIdGenerator\n$gradleFile")

      var result = TestUtils.executeTask("generateApolloSources", dir)

      Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":generateApolloSources")!!.outcome)

      File(dir, "build.gradle").replaceInText("queryString.length()", "(queryString.length() * 2)")

      result = TestUtils.executeTask("generateApolloSources", dir)

      Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":generateApolloSources")!!.outcome)
    }
  }
}