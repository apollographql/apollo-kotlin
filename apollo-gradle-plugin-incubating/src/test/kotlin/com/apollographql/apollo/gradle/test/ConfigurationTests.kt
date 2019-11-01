package com.apollographql.apollo.gradle.test

import com.apollographql.apollo.compiler.child
import com.apollographql.apollo.gradle.util.TestUtils
import com.apollographql.apollo.gradle.util.TestUtils.withSimpleProject
import com.apollographql.apollo.gradle.util.generatedChild
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.hamcrest.CoreMatchers.containsString
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ConfigurationTests {
  @Test
  fun `customTypeMapping is working`() {
    withSimpleProject("""
      apollo {
        customTypeMapping = ["DateTime": "java.util.Date"]
        suppressRawTypesWarning = true
      }
    """.trimIndent()) { dir ->
      TestUtils.executeTask("generateApolloSources", dir)
      TestUtils.assertFileContains(dir, "main/service0/com/example/type/CustomType.java", "return Date.class;")
    }
  }

  @Test
  fun `nullableValueType is working`() {
    for (pair in listOf(
        "annotated" to "@Nullable String name()",
        "apolloOptional" to "import com.apollographql.apollo.api.internal.Optional;",
        "guavaOptional" to "import com.google.common.base.Optional;",
        "javaOptional" to "import java.util.Optional;",
        "inputType" to "Input<String> name()"
    )) {
      withSimpleProject("""
      apollo {
        nullableValueType = "${pair.first}"
      }
    """.trimIndent()) { dir ->
        TestUtils.executeTask("generateApolloSources", dir)
        TestUtils.assertFileContains(dir, "main/service0/com/example/DroidDetailsQuery.java", pair.second)
      }
    }
  }

  @Test
  fun `useSemanticNaming defaults to true`() {
    withSimpleProject("""
    """.trimIndent()) { dir ->
      TestUtils.executeTask("generateApolloSources", dir)
      TestUtils.assertFileContains(dir, "main/service0/com/example/DroidDetailsQuery.java", "class DroidDetailsQuery ")
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
      TestUtils.assertFileContains(dir, "main/service0/com/example/DroidDetails.java", "class DroidDetails ")
    }
  }

  @Test
  fun `generateModelBuilders defaults to false`() {
    withSimpleProject("""
    """.trimIndent()) { dir ->
      TestUtils.executeTask("generateApolloSources", dir)
      TestUtils.assertFileDoesNotContain(dir, "main/service0/com/example/DroidDetailsQuery.java", "Builder toBuilder()")
    }
  }

  @Test
  fun `generateModelBuilders generates builders correctly`() {
    withSimpleProject("""
      apollo {
        generateModelBuilder = true
      }
    """.trimIndent()) { dir ->
      TestUtils.executeTask("generateApolloSources", dir)
      TestUtils.assertFileContains(dir, "main/service0/com/example/DroidDetailsQuery.java", "Builder toBuilder()")
    }
  }

  @Test
  fun `useJavaBeansSemanticNaming defaults to false`() {
    withSimpleProject("""
    """.trimIndent()) { dir ->
      TestUtils.executeTask("generateApolloSources", dir)
      TestUtils.assertFileContains(dir, "main/service0/com/example/DroidDetailsQuery.java", "String name()")
    }
  }

  @Test
  fun `useJavaBeansSemanticNaming generates java beans methods correctly`() {
    withSimpleProject("""
      apollo {
        useJavaBeansSemanticNaming = true
      }
    """.trimIndent()) { dir ->
      TestUtils.executeTask("generateApolloSources", dir)
      TestUtils.assertFileContains(dir, "main/service0/com/example/DroidDetailsQuery.java", "String getName()")
    }
  }

  @Test
  fun `schemaFilePath fails`() {
    withSimpleProject("""
      apollo {
        schemaFilePath = "schema.json"
      }
    """.trimIndent()) { dir ->
      var exception: Exception? = null
      try {
        TestUtils.executeTask("generateApolloSources", dir)
      } catch (e: UnexpectedBuildFailure) {
        exception = e
        assertThat(e.message, containsString("is not supported anymore"))
      }
      assertNotNull(exception)
    }
  }

  @Test
  fun `outputPackageName fails`() {
    withSimpleProject("""
      apollo {
        outputPackageName = "com.starwars"
      }
    """.trimIndent()) { dir ->
      var exception: Exception? = null
      try {
        TestUtils.executeTask("generateApolloSources", dir)
      } catch (e: UnexpectedBuildFailure) {
        exception = e
        assertThat(e.message, containsString("is not supported anymore"))
      }
      assertNotNull(exception)
    }
  }

  @Test
  fun `sourceSet fails gracefully`() {
    withSimpleProject("""
      apollo {
        sourceSet {
          schemaFile = "schema.json"
          exclude = "**/*.gql"
        }
      }
    """.trimIndent()) { dir ->
      var exception: Exception? = null
      try {
        TestUtils.executeTask("generateApolloSources", dir)
      } catch (e: UnexpectedBuildFailure) {
        exception = e
        assertThat(e.message, containsString("is not supported anymore"))
      }
      assertNotNull(exception)
    }
  }

  @Test
  fun `sourceSet with multiple exclude fails gracefully`() {
    withSimpleProject("""
      apollo {
        sourceSet {
          schemaFile = "schema.json"
          exclude = ["**/Query1.graphql", "**/Query2.graphql"]
        }
      }
    """.trimIndent()) { dir ->
      var exception: Exception? = null
      try {
        TestUtils.executeTask("generateApolloSources", dir)
      } catch (e: UnexpectedBuildFailure) {
        exception = e
        assertThat(e.message, containsString("is not supported anymore"))
      }
      assertNotNull(exception)
    }
  }

  @Test
  fun `service compilerParams override extension compilerParams`() {
    withSimpleProject("""
      apollo {
        useSemanticNaming = false
        service("starwars") {
          useSemanticNaming = true
          schemaPath("com/example/schema.json")
        }
      }
    """.trimIndent()) { dir ->
      TestUtils.executeTask("generateApolloSources", dir)
      TestUtils.assertFileContains(dir, "main/starwars/com/example/DroidDetailsQuery.java", "class DroidDetailsQuery ")
    }
  }


  @Test
  fun `sourceFolder can be changed`() {
    withSimpleProject("""
      apollo {
        service("starwars") {
          sourceFolder("starwars")
        }
      }
    """.trimIndent()) { dir ->
      File(dir, "src/main/graphql/com/example").copyRecursively(File(dir, "src/main/graphql/starwars"))
      File(dir, "src/main/graphql/com").deleteRecursively()

      TestUtils.executeTask("generateApolloSources", dir)
      assertTrue(dir.generatedChild("main/starwars/DroidDetailsQuery.java").isFile)
      assertTrue(dir.generatedChild("main/starwars/type/CustomType.java").isFile)
      assertTrue(dir.generatedChild("main/starwars/fragment/SpeciesInformation.java").isFile)
    }
  }

  @Test
  fun `rootPackageName works as expected`() {
    withSimpleProject("""
      apollo {
        service("service") {
          rootPackageName = "com.starwars"
        }
      }
    """.trimIndent()) { dir ->
      TestUtils.executeTask("generateApolloSources", dir)
      assertTrue(dir.generatedChild("main/service/com/starwars/com/example/DroidDetailsQuery.java").isFile)
      assertTrue(dir.generatedChild("main/service/com/starwars/com/example/type/CustomType.java").isFile)
      assertTrue(dir.generatedChild("main/service/com/starwars/com/example/fragment/SpeciesInformation.java").isFile)
    }
  }

  @Test
  fun `exclude is working properly`() {
    withSimpleProject("""
      apollo {
        service("starwars") {
          schemaPath("com/example/schema.json")
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
  fun `generateTransformedQueries generates queries with __typename`() {
    withSimpleProject("""
      apollo {
        generateTransformedQueries = true
      }
    """.trimIndent()) { dir ->
      val result = TestUtils.executeTask("generateApolloSources", dir)

      assertEquals(TaskOutcome.SUCCESS, result.task(":generateApolloSources")!!.outcome)
      val transformedQuery = dir.child("build", "generated", "transformedQueries", "apollo", "main", "service0", "com", "example", "DroidDetails.graphql")
      assertThat(transformedQuery.readText(), containsString("__typename"))
    }
  }

  @Test
  fun `compilation unit directory properties carry task dependencies`() {
    withSimpleProject("""
      apollo {
        generateTransformedQueries = true
        
        compilationUnits.all { compilationUnit ->
          tasks.register("customTask" + compilationUnit.name) {
            inputs.dir(compilationUnit.outputDir)
            inputs.dir(compilationUnit.transformedQueriesDir)
          }
        }
      }
    """.trimIndent()) { dir ->
      val result = TestUtils.executeTask("customTaskmainservice0", dir)

      assertEquals(TaskOutcome.SUCCESS, result.task(":generateMainService0ApolloSources")!!.outcome)
    }
  }
}
