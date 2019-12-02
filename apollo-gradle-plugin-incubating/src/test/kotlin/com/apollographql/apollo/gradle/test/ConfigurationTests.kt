package com.apollographql.apollo.gradle.test

import com.apollographql.apollo.gradle.internal.child
import com.apollographql.apollo.gradle.util.TestUtils
import com.apollographql.apollo.gradle.util.TestUtils.withSimpleProject
import com.apollographql.apollo.gradle.util.generatedChild
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.hamcrest.CoreMatchers.containsString
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files

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
      TestUtils.assertFileContains(dir, "main/service/com/example/type/CustomType.java", "return Date.class;")
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
        TestUtils.assertFileContains(dir, "main/service/com/example/DroidDetailsQuery.java", pair.second)
      }
    }
  }

  @Test
  fun `useSemanticNaming defaults to true`() {
    withSimpleProject("""
    """.trimIndent()) { dir ->
      TestUtils.executeTask("generateApolloSources", dir)
      TestUtils.assertFileContains(dir, "main/service/com/example/DroidDetailsQuery.java", "class DroidDetailsQuery ")
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
      TestUtils.assertFileContains(dir, "main/service/com/example/DroidDetails.java", "class DroidDetails ")
    }
  }

  @Test
  fun `generateModelBuilders defaults to false`() {
    withSimpleProject("""
    """.trimIndent()) { dir ->
      TestUtils.executeTask("generateApolloSources", dir)
      TestUtils.assertFileDoesNotContain(dir, "main/service/com/example/DroidDetailsQuery.java", "Builder toBuilder()")
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
      TestUtils.assertFileContains(dir, "main/service/com/example/DroidDetailsQuery.java", "Builder toBuilder()")
    }
  }

  @Test
  fun `useJavaBeansSemanticNaming defaults to false`() {
    withSimpleProject("""
    """.trimIndent()) { dir ->
      TestUtils.executeTask("generateApolloSources", dir)
      TestUtils.assertFileContains(dir, "main/service/com/example/DroidDetailsQuery.java", "String name()")
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
      TestUtils.assertFileContains(dir, "main/service/com/example/DroidDetailsQuery.java", "String getName()")
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
      assertTrue(dir.generatedChild("main/service/com/starwars/com/example/DroidDetailsQuery.java").isFile)
      assertTrue(dir.generatedChild("main/service/com/starwars/com/example/type/CustomType.java").isFile)
      assertTrue(dir.generatedChild("main/service/com/starwars/com/example/fragment/SpeciesInformation.java").isFile)
    }
  }

  @Test
  fun `schemaFilePath with absolute path fails`() {
    withSimpleProject("""
      apollo {
        schemaFilePath = "/home/apollographql/schema.json"
      }
    """.trimIndent()) { dir ->
      var exception: Exception? = null
      try {
        TestUtils.executeTask("generateApolloSources", dir)
      } catch (e: UnexpectedBuildFailure) {
        exception = e
        assertThat(e.message, containsString("schemaPath = \"/home/apollographql/schema.json\""))
      }
      assertNotNull(exception)
    }
  }

  @Test
  fun `schemaFilePath with relative path fails`() {
    withSimpleProject("""
      apollo {
        schemaFilePath = "src/main/graphql/schema.json"
      }
    """.trimIndent()) { dir ->
      var exception: Exception? = null
      try {
        TestUtils.executeTask("generateApolloSources", dir)
      } catch (e: UnexpectedBuildFailure) {
        exception = e
        assertThat(e.message, containsString("schemaPath = \"schema.json\""))
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
          schemaPath = "com/example/schema.json"
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
          sourceFolder = "starwars"
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
  fun `schemaPath can be absolute path`() {
    val schema = File(System.getProperty("user.dir"), "src/test/files/starwars/schema.json")
    withSimpleProject("""
      apollo {
        service("starwars") {
          schemaPath = "${schema.absolutePath}"
        }
      }
    """.trimIndent()) { dir ->
      TestUtils.executeTask("generateApolloSources", dir)
      TestUtils.assertFileContains(dir, "main/starwars/com/example/DroidDetailsQuery.java", "class DroidDetailsQuery ")
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
      assertTrue(dir.generatedChild("main/starwars/DroidDetailsQuery.java").isFile)
      assertTrue(dir.generatedChild("main/starwars/type/CustomType.java").isFile)
      assertTrue(dir.generatedChild("main/starwars/fragment/SpeciesInformation.java").isFile)
    }
  }

  @Test
  fun `rootPackageName can be overridden in service`() {
    withSimpleProject("""
      apollo {
        rootPackageName = "com.something.else"
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
  fun `rootPackageName can be overridden in compilationUnits`() {
    withSimpleProject("""
      apollo {
        rootPackageName = "com.default"
        service("starwars") {
          rootPackageName = "com.starwars"
        }
        onCompilationUnits {
          rootPackageName = "com.overrides"
        }
      }
    """.trimIndent()) { dir ->
      TestUtils.executeTask("generateApolloSources", dir)
      assertTrue(dir.generatedChild("main/starwars/com/overrides/com/example/DroidDetailsQuery.java").isFile)
      assertTrue(dir.generatedChild("main/starwars/com/overrides/com/example/type/CustomType.java").isFile)
      assertTrue(dir.generatedChild("main/starwars/com/overrides/com/example/fragment/SpeciesInformation.java").isFile)
    }
  }

  @Test
  fun `sources can be overridden in compilationUnits`() {
    withSimpleProject("""
      apollo {
        service("starwars") {
          schemaPath = "com/some/other/schema.json"
          sourceFolder = "com/some/other"
        }
        
        onCompilationUnits {
          schemaFile = file("src/main/graphql/com/example/schema.json")
          graphqlSourceDirectorySet.srcDir(file("src/main/graphql/"))
          graphqlSourceDirectorySet.include("**/*.graphql")
        }
      }
    """.trimIndent()) { dir ->
      TestUtils.executeTask("generateApolloSources", dir)
      assertTrue(dir.generatedChild("main/starwars/com/example/DroidDetailsQuery.java").isFile)
      assertTrue(dir.generatedChild("main/starwars/com/example/type/CustomType.java").isFile)
      assertTrue(dir.generatedChild("main/starwars/com/example/fragment/SpeciesInformation.java").isFile)
    }
  }

  @Test
  fun `exclude is working properly`() {
    withSimpleProject("""
      apollo {
        service("starwars") {
          schemaPath = "com/example/schema.json"
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
      val transformedQuery = dir.child("build", "generated", "transformedQueries", "apollo", "main", "service", "com", "example", "DroidDetails.graphql")
      assertThat(transformedQuery.readText(), containsString("__typename"))
    }
  }

  @Test
  fun `compilation unit directory properties carry task dependencies`() {
    withSimpleProject("""
      apollo {
        generateTransformedQueries = true
        
        onCompilationUnits { compilationUnit ->
          tasks.register("customTask" + compilationUnit.name.capitalize()) {
            inputs.dir(compilationUnit.outputDir)
            inputs.dir(compilationUnit.transformedQueriesDir)
          }
        }
      }
    """.trimIndent()) { dir ->
      val result = TestUtils.executeTask("customTaskMainservice", dir)

      assertEquals(TaskOutcome.SUCCESS, result.task(":generateMainServiceApolloSources")!!.outcome)
    }
  }

  @Test
  fun `symlinks are not followed for the schema`() {
    withSimpleProject { dir ->
      dir.child("src/main/graphql/com/example/schema.json").copyTo(dir.child("schema.json"))
      dir.child("src/main/graphql/com/example/schema.json").delete()


      Files.createSymbolicLink(dir.child(
          "src/main/graphql/com/example/schema.json").toPath(),
          dir.child("schema.json").toPath()
      )

      TestUtils.executeTask("generateApolloSources", dir)

      assertTrue(dir.generatedChild("main/service/com/example/fragment/SpeciesInformation.java").isFile)
    }
  }

  @Test
  fun `symlinks are not followed for sources`() {
    withSimpleProject { dir ->
      dir.child("src/main/graphql/com/example").copyRecursively(dir.child("tmp"))
      dir.child("src/main/graphql/com/").deleteRecursively()


      Files.createSymbolicLink(
          dir.child("src/main/graphql/example").toPath(),
          dir.child("tmp").toPath()
      )

      TestUtils.executeTask("generateApolloSources", dir)

      assertTrue(dir.generatedChild("main/service/example/fragment/SpeciesInformation.java").isFile)
    }
  }
}
