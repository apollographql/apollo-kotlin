package com.apollographql.apollo3.gradle.test

import com.apollographql.apollo3.gradle.util.TestUtils
import com.apollographql.apollo3.gradle.util.TestUtils.withSimpleProject
import com.apollographql.apollo3.gradle.util.TestUtils.withTestProject
import com.apollographql.apollo3.gradle.util.generatedChild
import com.apollographql.apollo3.gradle.util.replaceInText
import junit.framework.Assert.assertTrue
import org.gradle.testkit.runner.TaskOutcome
import org.hamcrest.CoreMatchers
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import java.io.File

class OperationIdGeneratorTests {
  val apolloConfiguration = """
      class MyIdGenerator implements OperationIdGenerator {
          String apply(String queryString, String queryFilepath) {
              return queryString.length().toString();
          }
          String version = "MyIdGenerator-v1"
      }
      
      apollo {
        operationIdGenerator = new MyIdGenerator()
        packageNamesFromFilePaths()
      }
    """.trimIndent()

  @Test
  fun `up-to-date checks are working`() {
    withSimpleProject(apolloConfiguration = apolloConfiguration) {dir ->
      val gradleFile = File(dir, "build.gradle").readText()

      File(dir, "build.gradle").writeText("import com.apollographql.apollo3.compiler.OperationIdGenerator\n$gradleFile")

      var result = TestUtils.executeTask("generateApolloSources", dir)

      Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":generateApolloSources")!!.outcome)

      result = TestUtils.executeTask("generateApolloSources", dir)

      Assert.assertEquals(TaskOutcome.UP_TO_DATE, result.task(":generateApolloSources")!!.outcome)
    }
  }

  @Test
  fun `operationIdGenerator is working`() {
    val apolloConfiguration = """
      class MyIdGenerator implements OperationIdGenerator {
          String apply(String operationDocument, String operationName) {
              return operationName;
          }
          String version = "MyIdGenerator-v1"
      }
      
      apollo {
        packageNamesFromFilePaths()
        operationIdGenerator = new MyIdGenerator()
      }
    """.trimIndent()

    withSimpleProject(apolloConfiguration = apolloConfiguration) {dir ->
      val gradleFile = File(dir, "build.gradle").readText()

      File(dir, "build.gradle").writeText("import com.apollographql.apollo3.compiler.OperationIdGenerator\n$gradleFile")

      val result = TestUtils.executeTask("generateApolloSources", dir)

      Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":generateApolloSources")!!.outcome)

      val queryJavaFile = dir.generatedChild("service/com/example/DroidDetailsQuery.kt")
      assertTrue(queryJavaFile.readText().contains("DroidDetailsQuery"))
    }
  }

  @Test
  fun `changing the operationIdGenerator recompiles sources`() {
    withSimpleProject(apolloConfiguration = apolloConfiguration) { dir ->
      val gradleFile = File(dir, "build.gradle").readText()

      File(dir, "build.gradle").writeText("import com.apollographql.apollo3.compiler.OperationIdGenerator\n$gradleFile")

      var result = TestUtils.executeTask("generateApolloSources", dir)

      Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":generateApolloSources")!!.outcome)

      File(dir, "build.gradle").replaceInText("queryString.length().toString()", "queryString.length().toString() + \"(modified)\"")
      File(dir, "build.gradle").replaceInText("MyIdGenerator-v1", "MyIdGenerator-v2")

      result = TestUtils.executeTask("generateApolloSources", dir)

      Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":generateApolloSources")!!.outcome)
    }
  }

  @Test
  fun `build cache is working as expected`() {
    withSimpleProject(apolloConfiguration = apolloConfiguration) { dir ->
      val buildCachePath = File(dir, "buildCache").absolutePath
      File(dir, "settings.gradle").appendText("""
        
        buildCache {
            local {
                directory = new File("$buildCachePath")
            }
        }
      """.trimIndent())

      val gradleFile = File(dir, "build.gradle").readText()

      File(dir, "build.gradle").writeText("import com.apollographql.apollo3.compiler.OperationIdGenerator\n$gradleFile")

      var result = TestUtils.executeTask("generateServiceApolloSources", dir, "--build-cache", "-i")

      Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":generateServiceApolloSources")!!.outcome)

      File(dir, "build").deleteRecursively()

      result = TestUtils.executeTask("generateServiceApolloSources", dir, "--build-cache", "-i")

      Assert.assertEquals(TaskOutcome.FROM_CACHE, result.task(":generateServiceApolloSources")!!.outcome)
    }
  }

  @Test
  fun `operationOutputGenerator is working as expected`() {
    withTestProject("operationIds") { dir ->

      var result = TestUtils.executeTask("generateApolloSources", dir)

      Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":generateServiceApolloSources")!!.outcome)

      Assert.assertThat(
          dir.generatedChild("service/com/example/GreetingQuery.kt").readText(),
          CoreMatchers.containsString("OPERATION_ID: String = \"GreetingCustomId\"")
      )

      // Change the implementation of the operation ID generator and check again
      File(dir,"build.gradle.kts").replaceInText("CustomId", "anotherCustomId")
      File(dir,"build.gradle.kts").replaceInText("OperationOutputGenerator-v1", "OperationOutputGenerator-v2")

      result = TestUtils.executeTask("generateApolloSources", dir)

      Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":generateServiceApolloSources")!!.outcome)

      Assert.assertThat(
          dir.generatedChild("service/com/example/GreetingQuery.kt").readText(),
          CoreMatchers.containsString("anotherCustomId")
      )
    }
  }
}
