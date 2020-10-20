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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ConfigurationTests {
  @Test
  fun `customTypeMapping is working`() {
    withSimpleProject("""
      apollo {
        customTypeMapping = ["DateTime": "java.util.Date"]
      }
    """.trimIndent()) { dir ->
      TestUtils.executeTask("generateApolloSources", dir)
      TestUtils.assertFileContains(dir, "service/com/example/type/CustomType.kt", "= \"java.util.Date\"")
    }
  }

  @Test
  fun `customTypeMapping can be applied from a service block`() {
    withSimpleProject("""
      apollo {
        service("other") {
        }
        service("api") {
          customTypeMapping = ["DateTime": "java.util.Date"]
        }
      }
    """.trimIndent()) { dir ->
      TestUtils.executeTask("generateApolloSources", dir)
      TestUtils.assertFileContains(dir, "api/com/example/type/CustomType.kt", "= \"java.util.Date\"")
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
      TestUtils.assertFileContains(dir, "service/com/example/DroidDetails.kt", "class DroidDetails ")
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
      assertTrue(dir.generatedChild("service/com/starwars/com/example/type/CustomType.kt").isFile)
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
      assertTrue(dir.generatedChild("starwars/type/CustomType.kt").isFile)
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
      assertTrue(dir.generatedChild("starwars/type/CustomType.kt").isFile)
      assertTrue(dir.generatedChild("starwars/fragment/SpeciesInformation.kt").isFile)
    }
  }

  @Test
  fun `graphqlSourceDirectorySet overrides sourceFolder`() {
    withSimpleProject("""
      apollo {
        sourceFolder.set("non-existing")
        schemaFile = file("src/main/graphql/com/example/schema.json")
        graphqlSourceDirectorySet.srcDir(file("src/main/graphql/"))
        graphqlSourceDirectorySet.include("**/*.graphql")
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
      val expectedOperationId = "260dd8d889c94e78b975e435300929027d0ad10ea55b63695b13894eb8cd8578"
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
  fun `test variants do not create duplicate classes`() {
    withSimpleProject("""
      apollo {
        schemaFile = file("src/main/graphql/com/example/schema.json")
        graphqlSourceDirectorySet.srcDir("src/main/graphql/")
        graphqlSourceDirectorySet.include("**/*.graphql")
      }
    """.trimIndent()) { dir ->

      File(dir, "build.gradle").replaceInText(
          "implementation dep.apollo.api",
          "implementation dep.apollo.api\nimplementation \"junit:junit:4.12\""
      )
      File(dir, "src/test/java/com/example/").mkdirs()
      File(dir, "src/test/java/com/example/MainTest.java").writeText("""
        package com.example;
        
        import com.example.DroidDetailsQuery;
        import org.junit.Test;
        
        public class MainTest {
            @Test
            public void aMethodThatReferencesAGeneratedQuery() {
                new DroidDetailsQuery();
            }
        }
""".trimIndent())
      val result = TestUtils.executeTask("build", dir)

      assertEquals(TaskOutcome.SUCCESS, result.task(":build")!!.outcome)
    }
  }
}
