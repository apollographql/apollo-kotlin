package com.apollographql.apollo3.gradle.test

import com.apollographql.apollo3.gradle.util.TestUtils
import com.apollographql.apollo3.gradle.util.TestUtils.fixturesDirectory
import com.apollographql.apollo3.gradle.util.TestUtils.withSimpleProject
import com.apollographql.apollo3.gradle.util.TestUtils.withTestProject
import com.apollographql.apollo3.gradle.util.generatedChild
import com.apollographql.apollo3.gradle.util.replaceInText
import com.google.common.truth.Truth
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.hamcrest.CoreMatchers.containsString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ServiceTests {
  @Test
  fun `customScalarsMapping is working`() {
    withSimpleProject("""
      apollo {
        packageNamesFromFilePaths()
        customScalarsMapping = ["DateTime": "java.util.Date"]
      }
    """.trimIndent()) { dir ->
      TestUtils.executeTask("generateApolloSources", dir)
      TestUtils.assertFileContains(dir, "service/com/example/type/DateTime.kt", "\"java.util.Date\"")
    }
  }

  @Test
  fun `registering an unknown custom scalar fails`() {
    withSimpleProject("""
      apollo {
        packageNamesFromFilePaths()
        customScalarsMapping = ["UnknownScalar": "java.util.Date"]
      }
    """.trimIndent()) { dir ->
      try {
        TestUtils.executeTask("generateApolloSources", dir)
        fail("Registering an unknown scalar should fail")
      } catch (e: UnexpectedBuildFailure) {
        Truth.assertThat(e.message).contains("unknown custom scalar(s)")
      }
    }
  }


  @Test
  fun canConfigureOutputDir() {
    withSimpleProject("""
      apollo {
        packageNamesFromFilePaths()
        outputDir.set(file("build/apollo"))
      }
    """.trimIndent()) { dir ->
      TestUtils.executeTask("generateApolloSources", dir)
      assertTrue(File(dir, "build/apollo/com/example/type/DateTime.kt").exists())
    }
  }

  @Test
  fun `customScalarsMapping put is working`() {
    withSimpleProject("""
      apollo {
        packageNamesFromFilePaths()
        customScalarsMapping.put("DateTime", "java.util.Date")
      }
    """.trimIndent()) { dir ->
      TestUtils.executeTask("generateApolloSources", dir)
      TestUtils.assertFileContains(dir, "service/com/example/type/DateTime.kt", "\"java.util.Date\"")
    }
  }

  @Test
  fun `customScalarsMapping can be applied from a service block`() {
    withSimpleProject("""
      apollo {
        service("other") {
          packageNamesFromFilePaths()
        }
        service("api") {
          packageNamesFromFilePaths()
          customScalarsMapping = ["DateTime": "java.util.Date"]
        }
      }
    """.trimIndent()) { dir ->
      TestUtils.executeTask("generateApolloSources", dir)
      TestUtils.assertFileContains(dir, "api/com/example/type/DateTime.kt", "\"java.util.Date\"")
    }
  }

  @Test
  fun `useSemanticNaming defaults to true`() {
    withSimpleProject("""
      apollo {
        packageNamesFromFilePaths()
      }
    """.trimIndent()) { dir ->
      TestUtils.executeTask("generateApolloSources", dir)
      TestUtils.assertFileContains(dir, "service/com/example/DroidDetailsQuery.kt", "class DroidDetailsQuery")
    }
  }

  @Test
  fun `useSemanticNaming can be turned off correctly`() {
    withSimpleProject("""
      apollo {
        packageNamesFromFilePaths()
        useSemanticNaming = false
      }
    """.trimIndent()) { dir ->
      TestUtils.executeTask("generateApolloSources", dir)
      TestUtils.assertFileContains(dir, "service/com/example/DroidDetails.kt", "class DroidDetails")
    }
  }

  @Test
  fun `packageName works as expected`() {
    withSimpleProject("""
      apollo {
        packageName = "com.starwars"
      }
    """.trimIndent()) { dir ->
      TestUtils.executeTask("generateApolloSources", dir)
      assertTrue(dir.generatedChild("service/com/starwars/DroidDetailsQuery.kt").isFile)
      assertTrue(dir.generatedChild("service/com/starwars/type/DateTime.kt").isFile)
      assertTrue(dir.generatedChild("service/com/starwars/fragment/SpeciesInformation.kt").isFile)
    }
  }

  @Test
  fun `schemaFile can be an absolute path`() {
    val schema = File(System.getProperty("user.dir"), "src/test/files/starwars/schema.json")
    withSimpleProject("""
      apollo {
        service("starwars") {
          schemaFile.set(file("${schema.absolutePath}"))
          packageName.set("com.example")
        }
      }
    """.trimIndent()) { dir ->
      TestUtils.executeTask("generateApolloSources", dir)
      TestUtils.assertFileContains(dir, "starwars/com/example/DroidDetailsQuery.kt", "class DroidDetailsQuery")
    }
  }

  @Test
  fun `schemaFile can point to a schema file outside the module`() {
    withSimpleProject("""
      apollo {
        schemaFile = file("../schema.json")
        packageName.set("com.example")
      }
    """.trimIndent()) { dir ->
      val dest = File(dir, "../schema.json")
      File(dir, "src/main/graphql/com/example/schema.json").copyTo(dest, true)
      File(dir, "src/main/graphql/com/example/schema.json").delete()

      TestUtils.executeTask("generateApolloSources", dir)

      assertTrue(dir.generatedChild("service/com/example/DroidDetailsQuery.kt").isFile)
    }
  }

  @Test
  fun `exclude is working properly`() {
    withSimpleProject("""
      apollo {
        service("starwars") {
          packageNamesFromFilePaths()
          excludes = ["**/*.gql"]
        }
      }
    """.trimIndent()) { dir ->
      File(dir, "src/main/graphql/com/example/error.gql").writeText("this is not valid graphql")
      File(dir, "src/main/graphql/com/example/error/").mkdir()
      File(dir, "src/main/graphql/com/example/error/error.gql").writeText("this is not valid graphql")
      val result = TestUtils.executeTask("generateApolloSources", dir)

      assertEquals(TaskOutcome.SUCCESS, result.task(":generateApolloSources")!!.outcome)
    }
  }

  @Test
  fun `versions are enforced`() {
    withSimpleProject { dir ->
      File(dir, "build.gradle").replaceInText("dep.apollo.api", "\"com.apollographql.apollo3:apollo-api:1.2.0\"")

      var exception: Exception? = null
      try {
        TestUtils.executeTask("generateApolloSources", dir)
      } catch (e: UnexpectedBuildFailure) {
        exception = e
        assertThat(e.message, containsString("All apollo versions should be the same"))
      }
      assertNotNull(exception)
    }
  }

  @Test
  fun `changing a dependency checks versions again`() {
    withSimpleProject { dir ->

      TestUtils.executeTaskAndAssertSuccess(":generateApolloSources", dir)

      val result = TestUtils.executeTask("checkApolloVersions", dir)
      assert(result.task(":checkApolloVersions")?.outcome == TaskOutcome.UP_TO_DATE)

      File(dir, "build.gradle").replaceInText("dep.apollo.api", "\"com.apollographql.apollo3:apollo-api:1.2.0\"")

      var exception: Exception? = null
      try {
        TestUtils.executeTask("checkApolloVersions", dir)
      } catch (e: UnexpectedBuildFailure) {
        exception = e
        assertThat(e.message, containsString("All apollo versions should be the same"))
      }
      assertNotNull(exception)
    }
  }

  @Test
  fun `versions are enforced even in rootProject`() {
    withTestProject("mismatchedVersions") { dir ->
      var exception: Exception? = null
      try {
        TestUtils.executeTask("generateApolloSources", dir)
      } catch (e: UnexpectedBuildFailure) {
        exception = e
        assertThat(e.message, containsString("All apollo versions should be the same"))
      }
      assertNotNull(exception)
    }
  }

  @Test
  fun `operationOutput generates queries with __typename`() {
    withSimpleProject("""
      apollo {
        packageNamesFromFilePaths()
        generateOperationOutput.set(true)
      }
    """.trimIndent()) { dir ->
      val result = TestUtils.executeTask("generateApolloSources", dir)

      assertEquals(TaskOutcome.SUCCESS, result.task(":generateApolloSources")!!.outcome)
      val operationOutput = File(dir, "build/generated/operationOutput/apollo/service/OperationOutput.json")

      // Check that the filename case did not change. See https://github.com/apollographql/apollo-android/issues/2533
      assertTrue(operationOutput.canonicalFile.path.endsWith("build/generated/operationOutput/apollo/service/operationOutput.json"))

      assertThat(operationOutput.readText(), containsString("__typename"))
    }
  }

  @Test
  fun `operationOutput uses same id as the query`() {
    withSimpleProject("""
      apollo {
        packageNamesFromFilePaths()
        generateOperationOutput.set(true)
      }
    """.trimIndent()) { dir ->
      val result = TestUtils.executeTask("generateApolloSources", dir)

      assertEquals(TaskOutcome.SUCCESS, result.task(":generateApolloSources")!!.outcome)
      val expectedOperationId = "292319c237e71c9dfec7a7d7f993e9c91bd81361a786f251840e105f4b6c9145"
      val operationOutput = File(dir, "build/generated/operationOutput/apollo/service/operationOutput.json")
      assertThat(operationOutput.readText(), containsString(expectedOperationId))

      val queryJavaFile = dir.generatedChild("service/com/example/DroidDetailsQuery.kt")
      assertThat(queryJavaFile.readText(), containsString(expectedOperationId))
    }
  }

  @Test
  fun `operationOutputFile carries task dependencies`() {
    withSimpleProject("""
      apollo { 
        generateOperationOutput.set(true)
        packageNamesFromFilePaths()
        operationOutputConnection {
          tasks.register("customTaskService") {
            inputs.file(operationOutputFile)
          }
        }
      }
    """.trimIndent()) { dir ->
      val result = TestUtils.executeTask("customTaskService", dir)

      assertEquals(TaskOutcome.SUCCESS, result.task(":generateServiceApolloSources")!!.outcome)
    }
  }

  @Test
  fun `symlinks are followed for the schema`() {
    withSimpleProject { dir ->
      File(dir, "src/main/graphql/com/example/schema.json").copyTo(File(dir, "schema.json"))
      File(dir, "src/main/graphql/com/example/schema.json").delete()

      Files.createSymbolicLink(
          File(dir, "src/main/graphql/com/example/schema.json").toPath(),
          File(dir, "schema.json").toPath()
      )

      TestUtils.executeTask("generateApolloSources", dir)

      assertTrue(dir.generatedChild("service/com/example/fragment/SpeciesInformation.kt").isFile)
    }
  }

  @Test
  fun `symlinks are followed for GraphQL sources`() {
    withSimpleProject { dir ->
      File(dir, "src/main/graphql/com/example").copyRecursively(File(dir, "tmp"))
      File(dir, "src/main/graphql/com/").deleteRecursively()

      Files.createSymbolicLink(
          File(dir, "src/main/graphql/example").toPath(),
          File(dir, "tmp").toPath()
      )

      TestUtils.executeTask("generateApolloSources", dir)

      assertTrue(dir.generatedChild("service/example/fragment/SpeciesInformation.kt").isFile)
    }
  }

  @Test
  fun `outputDirConnection can connect to the test source set`() {
    withTestProject("testSourceSet") { dir ->
      TestUtils.executeTask("build", dir)

      assertTrue(dir.generatedChild("service/com/example/GreetingQuery.kt").isFile)
      assertTrue(File(dir, "build/libs/testProject.jar").isFile)
    }
  }

  @Test
  fun `when generateAsInternal set to true - generated models are internal`() {
    val apolloConfiguration = """
      apollo {
        packageNamesFromFilePaths()
        generateAsInternal = true
      }
    """.trimIndent()

    TestUtils.withProject(
        usesKotlinDsl = false,
        apolloConfiguration = apolloConfiguration,
        plugins = listOf(TestUtils.kotlinJvmPlugin, TestUtils.apolloPlugin)
    ) { dir ->

      val source = fixturesDirectory()
      File(source, "kotlin").copyRecursively(File(dir, "src/main/kotlin"))

      TestUtils.executeTask("build", dir)

      assertTrue(dir.generatedChild("service/com/example/DroidDetailsQuery.kt").isFile)
      assertThat(dir.generatedChild("service/com/example/DroidDetailsQuery.kt").readText(), containsString("internal class"))

      assertTrue(dir.generatedChild("service/com/example/type/DateTime.kt").isFile)
      assertThat(dir.generatedChild("service/com/example/type/DateTime.kt").readText(), containsString("internal class DateTime"))

      assertTrue(dir.generatedChild("service/com/example/fragment/SpeciesInformation.kt").isFile)
      assertThat(dir.generatedChild("service/com/example/fragment/SpeciesInformation.kt").readText(), containsString("internal data class"))
    }
  }

  @Test
  fun `when generateFragmentImplementations is not set, it defaults to false`() {
    withSimpleProject { dir ->
      TestUtils.executeTask("generateApolloSources", dir)
      assertFalse(dir.generatedChild("service/com/example/fragment/SpeciesInformationImpl.kt").exists())
    }
  }

  @Test
  fun `when generateFragmentImplementations set to true, it generates default fragment implementation`() {
    withSimpleProject("""
      apollo {
        packageNamesFromFilePaths()
        generateFragmentImplementations = true
      }
    """.trimIndent()) { dir ->
      TestUtils.executeTask("generateApolloSources", dir)
      assertTrue(dir.generatedChild("service/com/example/fragment/SpeciesInformationImpl.kt").isFile)
    }
  }
}
