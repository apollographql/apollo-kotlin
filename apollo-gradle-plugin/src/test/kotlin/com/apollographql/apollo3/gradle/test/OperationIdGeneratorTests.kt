package com.apollographql.apollo.gradle.test

import com.apollographql.apollo.gradle.util.TestUtils
import com.apollographql.apollo.gradle.util.TestUtils.withSimpleProject
import com.apollographql.apollo.gradle.util.TestUtils.withTestProject
import com.apollographql.apollo.gradle.util.generatedChild
import com.apollographql.apollo.gradle.util.replaceInText
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
      }
    """.trimIndent()

  @Test
  fun `up-to-date checks are working`() {
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
  // filePath is not passed at the moment
  @Ignore
  fun `operationIdGenerator is working`() {
    val apolloConfiguration = """
      class MyIdGenerator implements OperationIdGenerator {
          String apply(String queryString, String queryFilepath) {
              return queryFilepath;
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

      val result = TestUtils.executeTask("generateApolloSources", dir)

      Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":generateApolloSources")!!.outcome)

      val queryJavaFile = dir.generatedChild("service/com/example/DroidDetailsQuery.kt")
      Assert.assertThat(queryJavaFile.readText(), CoreMatchers.containsString("com/example/DroidDetails.graphql"))
    }
  }

  @Test
  fun `changing the operationIdGenerator recompiles sources`() {
    withSimpleProject(apolloConfiguration = apolloConfiguration) { dir ->
      val gradleFile = File(dir, "build.gradle").readText()

      File(dir, "build.gradle").writeText("import com.apollographql.apollo.compiler.OperationIdGenerator\n$gradleFile")

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

      File(dir, "build.gradle").writeText("import com.apollographql.apollo.compiler.OperationIdGenerator\n$gradleFile")

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
