package com.apollographql.apollo.gradle.test

import util.TestUtils
import util.TestUtils.fixturesDirectory
import util.TestUtils.withSimpleProject
import util.TestUtils.withTestProject
import util.generatedChild
import util.replaceInText
import com.google.common.truth.Truth
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ServiceTests {
  @Test
  fun `mapScalar is working`() {
    withSimpleProject("""
      apollo {
        service("service") {
          packageNamesFromFilePaths()
          mapScalar("DateTime", "java.util.Date")
        }
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
        service("service") {
          packageNamesFromFilePaths()
          mapScalar("UnknownScalar", "java.util.Date")
        }
      }
    """.trimIndent()) { dir ->
      try {
        TestUtils.executeTask("generateApolloSources", dir)
        fail("Registering an unknown scalar should fail")
      } catch (e: UnexpectedBuildFailure) {
        Truth.assertThat(e.message).contains("unknown scalar(s)")
      }
    }
  }


  @Test
  fun canConfigureOutputDir() {
    withSimpleProject("""
      apollo {
        service("service") {
          packageNamesFromFilePaths()
          outputDir.set(file("build/apollo"))
        }
      }
    """.trimIndent()) { dir ->
      TestUtils.executeTask("generateApolloSources", dir)
      assertTrue(File(dir, "build/apollo/com/example/type/DateTime.kt").exists())
    }
  }

  @Test
  fun `useSemanticNaming defaults to true`() {
    withSimpleProject("""
      apollo {
        service("service") {
          packageNamesFromFilePaths()
        }
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
        service("service") {
          packageNamesFromFilePaths()
          useSemanticNaming = false
        }
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
        service("service") {
          packageName = "com.starwars"
        }
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
    val schema = File(System.getProperty("user.dir"), "testFiles/starwars/schema.json")
    withSimpleProject("""
      apollo {
        service("starwars") {
          schemaFiles.from(file("${schema.absolutePath}"))
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
        service("service") {
          schemaFile = file("../schema.json")
          packageName.set("com.example")
        }
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
      File(dir, "build.gradle").replaceInText("libs.apollo.api", "\"com.apollographql.apollo:apollo-api:1.2.0\"")

      var exception: Exception? = null
      try {
        TestUtils.executeTask("generateApolloSources", dir)
      } catch (e: UnexpectedBuildFailure) {
        exception = e
        Truth.assertThat(e.message).contains("All apollo versions should be the same")
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

      File(dir, "build.gradle").replaceInText("libs.apollo.api", "\"com.apollographql.apollo:apollo-api:1.2.0\"")

      try {
        TestUtils.executeTask("checkApolloVersions", dir)
        fail("An exception was expected")
      } catch (e: UnexpectedBuildFailure) {
        Truth.assertThat(e.message).contains("All apollo versions should be the same")
      }
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
        Truth.assertThat(e.message).contains("All apollo versions should be the same")
      }
      assertNotNull(exception)
    }
  }

  @Test
  fun `dependencies are using the plugin version by default`() {
    withTestProject("defaultVersion") { dir ->
      TestUtils.executeTaskAndAssertSuccess(":generateApolloSources", dir)
    }
  }

  @Test
  fun `legacy js target is not supported`() {
    withTestProject("legacyJsTarget") { dir ->
      var exception: Exception? = null
      try {
        TestUtils.executeTask("generateApolloSources", dir)
      } catch (e: UnexpectedBuildFailure) {
        exception = e
        Truth.assertThat(e.message).contains("Apollo: LEGACY js target is not supported by Apollo, please use IR.")
      }
      assertNotNull(exception)
    }
  }

  @Test
  fun `operationOutput uses same id as the query`() {
    withSimpleProject("""
      apollo {
        service("service") {
          packageNamesFromFilePaths()
          generateOperationOutput.set(true)
        }
      }
    """.trimIndent()) { dir ->
      val result = TestUtils.executeTask("generateApolloSources", dir)

      assertEquals(TaskOutcome.SUCCESS, result.task(":generateApolloSources")!!.outcome)
      val expectedOperationId = "292319c237e71c9dfec7a7d7f993e9c91bd81361a786f251840e105f4b6c9145"
      val operationOutput = File(dir, "build/generated/manifest/apollo/service/operationOutput.json")
      Truth.assertThat(operationOutput.readText()).contains(expectedOperationId)

      val queryJavaFile = dir.generatedChild("service/com/example/DroidDetailsQuery.kt")
      Truth.assertThat(queryJavaFile.readText()).contains(expectedOperationId)
    }
  }

  @Test
  fun `operationOutputFile carries task dependencies`() {
    withSimpleProject("""
      apollo { 
        service("service") {
          generateOperationOutput.set(true)
          packageNamesFromFilePaths()
          operationOutputConnection {
            tasks.register("customTaskService") {
              inputs.file(operationOutputFile)
            }
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
      assertTrue(File(dir, "build/libs/testSourceSet.jar").isFile)
    }
  }

  @Test
  fun `when generateAsInternal set to true - generated models are internal`() {
    val apolloConfiguration = """
      apollo {
        service("service") {
          packageNamesFromFilePaths()
          generateAsInternal = true
        }
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
      Truth.assertThat(dir.generatedChild("service/com/example/DroidDetailsQuery.kt").readText()).contains("internal class")

      assertTrue(dir.generatedChild("service/com/example/type/DateTime.kt").isFile)
      Truth.assertThat(dir.generatedChild("service/com/example/type/DateTime.kt").readText()).contains("internal class DateTime")

      assertTrue(dir.generatedChild("service/com/example/fragment/SpeciesInformation.kt").isFile)
      Truth.assertThat(dir.generatedChild("service/com/example/fragment/SpeciesInformation.kt").readText()).contains("internal data class")
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
        service("service") {
          packageNamesFromFilePaths()
          generateFragmentImplementations = true
        }
      }
    """.trimIndent()) { dir ->
      TestUtils.executeTask("generateApolloSources", dir)
      assertTrue(dir.generatedChild("service/com/example/fragment/SpeciesInformationImpl.kt").isFile)
    }
  }
}
