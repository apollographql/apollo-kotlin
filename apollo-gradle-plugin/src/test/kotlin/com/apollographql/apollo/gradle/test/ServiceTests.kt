package com.apollographql.apollo.gradle.test


import com.apollographql.apollo.gradle.util.TestUtils
import com.apollographql.apollo.gradle.util.TestUtils.fixturesDirectory
import com.apollographql.apollo.gradle.util.TestUtils.withSimpleProject
import com.apollographql.apollo.gradle.util.TestUtils.withTestProject
import com.apollographql.apollo.gradle.util.generatedChild
import com.apollographql.apollo.gradle.util.replaceInText
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.hamcrest.CoreMatchers.containsString
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ServiceTests {
  @Test
  fun `customScalarsMapping is working`() {
    withSimpleProject("""
      apollo {
        customScalarsMapping = ["DateTime": "java.util.Date"]
      }
    """.trimIndent()) { dir ->
      TestUtils.executeTask("generateApolloSources", dir)
      TestUtils.assertFileContains(dir, "service/com/example/type/CustomScalarType.kt", "= \"java.util.Date\"")
    }
  }

  @Test
  fun `customScalarsMapping put is working`() {
    withSimpleProject("""
      apollo {
        customScalarsMapping.put("DateTime", "java.util.Date")
      }
    """.trimIndent()) { dir ->
      TestUtils.executeTask("generateApolloSources", dir)
      TestUtils.assertFileContains(dir, "service/com/example/type/CustomScalarType.kt", "= \"java.util.Date\"")
    }
  }

  @Test
  fun `customScalarsMapping can be applied from a service block`() {
    withSimpleProject("""
      apollo {
        service("other") {
        }
        service("api") {
          customScalarsMapping = ["DateTime": "java.util.Date"]
        }
      }
    """.trimIndent()) { dir ->
      TestUtils.executeTask("generateApolloSources", dir)
      TestUtils.assertFileContains(dir, "api/com/example/type/CustomScalarType.kt", "= \"java.util.Date\"")
    }
  }


  @Test
  fun `useSemanticNaming defaults to true`() {
    withSimpleProject("""
    """.trimIndent()) { dir ->
      TestUtils.executeTask("generateApolloSources", dir)
      TestUtils.assertFileContains(dir, "service/com/example/DroidDetailsQuery.kt", "class DroidDetailsQuery ")
    }
  }

  @Test
  fun `useSemanticNaming can be turned off correctly`() {
    withSimpleProject("""
      apollo {
        useSemanticNaming = false
      }
    """.trimIndent()) { dir ->
      TestUtils.executeTask("generateApolloSources", dir)
      TestUtils.assertFileContains(dir, "service/com/example/DroidDetail.kt", "class DroidDetail ")
    }
  }

  @Test
  fun `rootPackageName works as expected`() {
    withSimpleProject("""
      apollo {
        rootPackageName = "com.starwars"
      }
    """.trimIndent()) { dir ->
      TestUtils.executeTask("generateApolloSources", dir)
      assertTrue(dir.generatedChild("service/com/starwars/com/example/DroidDetailsQuery.kt").isFile)
      assertTrue(dir.generatedChild("service/com/starwars/com/example/type/CustomScalarType.kt").isFile)
      assertTrue(dir.generatedChild("service/com/starwars/com/example/fragment/SpeciesInformation.kt").isFile)
    }
  }

  @Test
  fun `sourceFolder can be changed`() {
    withSimpleProject("""
      apollo {
        service("starwars") {
          sourceFolder = "starwars"
        }
      }
    """.trimIndent()) { dir ->
      File(dir, "src/main/graphql/com/example").copyRecursively(File(dir, "src/main/graphql/starwars"))
      File(dir, "src/main/graphql/com").deleteRecursively()

      TestUtils.executeTask("generateApolloSources", dir)
      assertTrue(dir.generatedChild("starwars/DroidDetailsQuery.kt").isFile)
      assertTrue(dir.generatedChild("starwars/type/CustomScalarType.kt").isFile)
      assertTrue(dir.generatedChild("starwars/fragment/SpeciesInformation.kt").isFile)
    }
  }

  @Test
  fun `schemaFile can be an absolute path`() {
    val schema = File(System.getProperty("user.dir"), "src/test/files/starwars/schema.json")
    withSimpleProject("""
      apollo {
        service("starwars") {
          schemaFile.set(file("${schema.absolutePath}"))
        }
      }
    """.trimIndent()) { dir ->
      TestUtils.executeTask("generateApolloSources", dir)
      TestUtils.assertFileContains(dir, "starwars/com/example/DroidDetailsQuery.kt", "class DroidDetailsQuery ")
    }
  }

  @Test
  fun `sourceFolder can be absolute path`() {
    val folder = File(System.getProperty("user.dir"), "src/test/files/starwars")
    withSimpleProject("""
      apollo {
        service("starwars") {
          sourceFolder = "${folder.absolutePath}"
        }
      }
    """.trimIndent()) { dir ->
      File(dir, "src/main/graphql/com").deleteRecursively()

      TestUtils.executeTask("generateApolloSources", dir)
      println(dir.absolutePath)
      dir.list()?.forEach(::println)
      assertTrue(dir.generatedChild("starwars/DroidDetailsQuery.kt").isFile)
      assertTrue(dir.generatedChild("starwars/type/CustomScalarType.kt").isFile)
      assertTrue(dir.generatedChild("starwars/fragment/SpeciesInformation.kt").isFile)
    }
  }

  @Test
  fun `graphqlSourceDirectorySet overrides sourceFolder`() {
    withSimpleProject("""
      apollo {
        sourceFolder.set("non-existing")
        schemaFile = file("src/main/graphql/com/example/schema.json")
        addGraphqlDirectory(file("src/main/graphql/"))
      }
    """.trimIndent()) { dir ->
      TestUtils.executeTask("generateApolloSources", dir)
      assertTrue(dir.generatedChild("service/com/example/DroidDetailsQuery.kt").isFile)
    }
  }

  @Test
  fun `schemaFile can point to a schema file outside the module`() {
    withSimpleProject("""
      apollo {
        schemaFile = file("../schema.json")
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
          exclude = ["**/*.gql"]
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
      File(dir, "build.gradle").replaceInText("dep.apollo.api", "\"com.apollographql.apollo:apollo-api:1.2.0\"")

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

      File(dir, "build.gradle").replaceInText("dep.apollo.api", "\"com.apollographql.apollo:apollo-api:1.2.0\"")

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
        withOperationOutput {
        }
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
        withOperationOutput {
        }
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
        withOperationOutput {
          tasks.register("customTask" + name.capitalize()) {
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
  fun `symlinks are not followed for the schema`() {
    withSimpleProject { dir ->
      File(dir, "src/main/graphql/com/example/schema.json").copyTo(File(dir, "schema.json"))
      File(dir, "src/main/graphql/com/example/schema.json").delete()


      Files.createSymbolicLink(File(dir, "src/main/graphql/com/example/schema.json").toPath(),
          File(dir, "schema.json").toPath()
      )

      TestUtils.executeTask("generateApolloSources", dir)

      assertTrue(dir.generatedChild("service/com/example/fragment/SpeciesInformation.kt").isFile)
    }
  }

  @Test
  fun `symlinks are not followed for sources`() {
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
  fun `withOutputDir can rewire to the test source set`() {
    withTestProject("testSourceSet") {dir ->
      TestUtils.executeTask("build", dir)

      assertTrue(dir.generatedChild("service/com/example/GreetingQuery.kt").isFile)
      assertTrue(File(dir, "build/libs/testProject.jar").isFile)
    }
  }

  @Test
  fun `when generateAsInternal set to true - generated models are internal`() {
    val apolloConfiguration = """
      apollo {
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

      assertTrue(dir.generatedChild("service/com/example/type/CustomScalarType.kt").isFile)
      assertThat(dir.generatedChild("service/com/example/type/CustomScalarType.kt").readText(), containsString("internal enum class"))

      assertTrue(dir.generatedChild("service/com/example/fragment/SpeciesInformation.kt").isFile)
      assertThat(dir.generatedChild("service/com/example/fragment/SpeciesInformation.kt").readText(), containsString("internal interface"))
    }
  }

  @Test
  fun `when sealedClassesForEnumsMatching to match all - generated enum type as sealed class`() {
    val apolloConfiguration = """
      apollo {
        service("githunt") {
          sourceFolder = "githunt"
          sealedClassesForEnumsMatching = [".*"]
        }
      }
    """.trimIndent()
    TestUtils.withProject(
        usesKotlinDsl = false,
        apolloConfiguration = apolloConfiguration,
        plugins = listOf(TestUtils.kotlinJvmPlugin, TestUtils.apolloPlugin)
    ) { dir ->
      val source = fixturesDirectory()

      val target = File(dir, "src/main/graphql/githunt")
      File(source, "githunt").copyRecursively(target = target, overwrite = true)

      File(dir, "src/main/graphql/com").deleteRecursively()

      TestUtils.executeTask("build", dir)

      assertTrue(dir.generatedChild("githunt/type/FeedType.kt").isFile)
      assertThat(dir.generatedChild("githunt/type/FeedType.kt").readText(), containsString("sealed class"))
    }
  }
}
