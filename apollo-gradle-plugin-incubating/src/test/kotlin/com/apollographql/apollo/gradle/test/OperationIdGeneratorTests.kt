package com.apollographql.apollo.gradle.test

import com.apollographql.apollo.gradle.util.TestUtils
import com.apollographql.apollo.gradle.util.TestUtils.withSimpleProject
import com.apollographql.apollo.gradle.util.replaceInText
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Test
import java.io.File

class OperationIdGeneratorTests {
  @Test
  fun `up-to-date checks are working`() {
    val apolloConfiguration = """
      class MyIdGenerator implements OperationIdGenerator {
          String apply(String operationDocument, String operationFilepath) {
              return operationDocument.length().toString();
          }
          String version = "1"
      }
      
      apollo {
        operationIdGenerator = new MyIdGenerator()
      }
    """.trimIndent()

    withSimpleProject(apolloConfiguration = apolloConfiguration) {dir ->
      val gradleFile = File(dir, "build.gradle").readText()

      File(dir, "build.gradle").writeText("import com.apollographql.apollo.compiler.OperationIdGenerator\n$gradleFile")

      var result = TestUtils.executeTask("generateApolloSources", dir)

      Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":generateApolloSources")!!.outcome)

      result = TestUtils.executeTask("generateApolloSources", dir)

      Assert.assertEquals(TaskOutcome.UP_TO_DATE, result.task(":generateApolloSources")!!.outcome)
    }
  }

  @Test
  fun `changing the operationIdGenerator recompiles sources`() {
    val apolloConfiguration = """
      class MyIdGenerator implements OperationIdGenerator {
          String apply(String operationDocument, String operationFilepath) {
              return operationDocument.length().toString();
          }
          String version = "MyIdGenerator-v1"
      }
      
      apollo {
        operationIdGenerator = new MyIdGenerator()
      }
    """.trimIndent()

    withSimpleProject(apolloConfiguration = apolloConfiguration) {dir ->
      val gradleFile = File(dir, "build.gradle").readText()

      File(dir, "build.gradle").writeText("import com.apollographql.apollo.compiler.OperationIdGenerator\n$gradleFile")

      var result = TestUtils.executeTask("generateApolloSources", dir)

      Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":generateApolloSources")!!.outcome)

      File(dir, "build.gradle").replaceInText("operationDocument.length()", "(operationDocument.length() * 2)")
      File(dir, "build.gradle").replaceInText("MyIdGenerator-v1", "MyIdGenerator-v2")

      result = TestUtils.executeTask("generateApolloSources", dir)

      Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":generateApolloSources")!!.outcome)
    }
  }
}
