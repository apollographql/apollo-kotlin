package com.apollographql.apollo.gradle.test

import com.google.common.truth.Truth
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Test
import util.TestUtils
import util.TestUtils.withSimpleProject
import util.TestUtils.withTestProject
import util.generatedChild
import util.replaceInText
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
        service("service") {
          operationIdGenerator = new MyIdGenerator()
          packageNamesFromFilePaths()
        }
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
  fun `operationIdGenerator is working`() {
    val apolloConfiguration = """
      class MyIdGenerator implements OperationIdGenerator {
          String apply(String operationDocument, String operationName) {
              return operationName;
          }
          String version = "MyIdGenerator-v1"
      }
      
      apollo {
        service("service") {
          packageNamesFromFilePaths()
          operationIdGenerator = new MyIdGenerator()
        }
      }
    """.trimIndent()

    withSimpleProject(apolloConfiguration = apolloConfiguration) {dir ->
      val gradleFile = File(dir, "build.gradle").readText()

      File(dir, "build.gradle").writeText("import com.apollographql.apollo.compiler.OperationIdGenerator\n$gradleFile")

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

      Truth.assertThat(dir.generatedChild("service/com/example/GreetingQuery.kt").readText())
          .contains("OPERATION_ID: String = \"GreetingCustomId\"")

      // Change the implementation of the operation ID generator and check again
      File(dir,"build.gradle.kts").replaceInText("CustomId", "anotherCustomId")
      File(dir,"build.gradle.kts").replaceInText("OperationOutputGenerator-v1", "OperationOutputGenerator-v2")

      result = TestUtils.executeTask("generateApolloSources", dir)

      Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":generateServiceApolloSources")!!.outcome)

      Truth.assertThat(dir.generatedChild("service/com/example/GreetingQuery.kt").readText())
          .contains("anotherCustomId")
    }
  }

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
