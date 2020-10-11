package com.apollographql.apollo.gradle.test

import com.apollographql.apollo.gradle.internal.child
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
      TestUtils.assertFileContains(dir, "main/service/com/example/type/CustomType.kt", "= \"java.util.Date\"")
    }
  }

  @Test
  fun `customTypeMapping can be applied from a service block`() {
    withSimpleProject("""
      apollo {
        service("other") {
          schemaPath = "com/example/schema.json"
        }
        service("api") {
          customTypeMapping = ["DateTime": "java.util.Date"]
          schemaPath = "com/example/schema.json"
        }
      }
    """.trimIndent()) { dir ->
      TestUtils.executeTask("generateApolloSources", dir)
      TestUtils.assertFileContains(dir, "main/api/com/example/type/CustomType.kt", "= \"java.util.Date\"")
    }
  }


  @Test
  fun `useSemanticNaming defaults to true`() {
    withSimpleProject("""
    """.trimIndent()) { dir ->
      TestUtils.executeTask("generateApolloSources", dir)
      TestUtils.assertFileContains(dir, "main/service/com/example/DroidDetailsQuery.kt", "class DroidDetailsQuery ")
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
      TestUtils.assertFileContains(dir, "main/service/com/example/DroidDetails.kt", "class DroidDetails ")
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
      assertTrue(dir.generatedChild("main/service/com/starwars/com/example/DroidDetailsQuery.kt").isFile)
      assertTrue(dir.generatedChild("main/service/com/starwars/com/example/type/CustomType.kt").isFile)
      assertTrue(dir.generatedChild("main/service/com/starwars/com/example/fragment/SpeciesInformation.kt").isFile)
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
      TestUtils.assertFileContains(dir, "main/starwars/com/example/DroidDetailsQuery.kt", "class DroidDetailsQuery ")
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
      assertTrue(dir.generatedChild("main/starwars/DroidDetailsQuery.kt").isFile)
      assertTrue(dir.generatedChild("main/starwars/type/CustomType.kt").isFile)
      assertTrue(dir.generatedChild("main/starwars/fragment/SpeciesInformation.kt").isFile)
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
      TestUtils.assertFileContains(dir, "main/starwars/com/example/DroidDetailsQuery.kt", "class DroidDetailsQuery ")
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
      assertTrue(dir.generatedChild("main/starwars/DroidDetailsQuery.kt").isFile)
      assertTrue(dir.generatedChild("main/starwars/type/CustomType.kt").isFile)
      assertTrue(dir.generatedChild("main/starwars/fragment/SpeciesInformation.kt").isFile)
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
      assertTrue(dir.generatedChild("main/service/com/starwars/com/example/DroidDetailsQuery.kt").isFile)
      assertTrue(dir.generatedChild("main/service/com/starwars/com/example/type/CustomType.kt").isFile)
      assertTrue(dir.generatedChild("main/service/com/starwars/com/example/fragment/SpeciesInformation.kt").isFile)
    }
  }

  @Test
  fun `rootPackageName can be overridden in compilationUnit`() {
    withSimpleProject("""
      apollo {
        rootPackageName = "com.default"
        service("starwars") {
          rootPackageName = "com.starwars"
        }
        onCompilationUnit {
          rootPackageName = "com.overrides"
        }
      }
    """.trimIndent()) { dir ->
      TestUtils.executeTask("generateApolloSources", dir)
      assertTrue(dir.generatedChild("main/starwars/com/overrides/com/example/DroidDetailsQuery.kt").isFile)
      assertTrue(dir.generatedChild("main/starwars/com/overrides/com/example/type/CustomType.kt").isFile)
      assertTrue(dir.generatedChild("main/starwars/com/overrides/com/example/fragment/SpeciesInformation.kt").isFile)
    }
  }

  @Test
  fun `sources with a service can be overridden in compilationUnit`() {
    withSimpleProject("""
      apollo {
        service("starwars") {
          schemaPath = "com/some/other/schema.json"
          sourceFolder = "com/some/other"
        }
        
        onCompilationUnit {
          schemaFile = file("src/main/graphql/com/example/schema.json")
          graphqlSourceDirectorySet.srcDir(file("src/main/graphql/"))
          graphqlSourceDirectorySet.include("**/*.graphql")
        }
      }
    """.trimIndent()) { dir ->
      TestUtils.executeTask("generateApolloSources", dir)
      assertTrue(dir.generatedChild("main/starwars/com/example/DroidDetailsQuery.kt").isFile)
      assertTrue(dir.generatedChild("main/starwars/com/example/type/CustomType.kt").isFile)
      assertTrue(dir.generatedChild("main/starwars/com/example/fragment/SpeciesInformation.kt").isFile)
    }
  }

  @Test
  fun `sources can be overridden in compilationUnit`() {
    withSimpleProject("""
      apollo {
        schemaFile = file("com/some/other/schema.json")

        onCompilationUnit {
          schemaFile = file("src/main/graphql/com/example/schema.json")
          graphqlSourceDirectorySet.srcDir(file("src/main/graphql/"))
          graphqlSourceDirectorySet.include("**/*.graphql")
        }
      }
    """.trimIndent()) { dir ->
      TestUtils.executeTask("generateApolloSources", dir)
      assertTrue(dir.generatedChild("main/service/com/example/DroidDetailsQuery.kt").isFile)
    }
  }

  @Test
  fun `onCompilationUnit can configure sources alone`() {
    withSimpleProject("""
      apollo {
        onCompilationUnit {
          schemaFile = file("src/main/graphql/com/example/schema.json")
          graphqlSourceDirectorySet.srcDir(file("src/main/graphql/"))
          graphqlSourceDirectorySet.include("**/*.graphql")
        }
      }
    """.trimIndent()) { dir ->
      TestUtils.executeTask("generateApolloSources", dir)
      assertTrue(dir.generatedChild("main/service/com/example/DroidDetailsQuery.kt").isFile)
    }
  }

  @Test
  fun `onCompilationUnit can point to a schema file outside the module`() {
    withSimpleProject("""
      apollo {
        onCompilationUnit {
          schemaFile = file("../schema.json")
        }
      }
    """.trimIndent()) { dir ->
      val dest = File(dir, "../schema.json")
      File(dir, "src/main/graphql/com/example/schema.json").copyTo(dest, true)
      TestUtils.executeTask("generateApolloSources", dir)
      dest.delete()
      assertTrue(dir.generatedChild("main/service/testProject/src/main/graphql/com/example/DroidDetailsQuery.kt").isFile)
    }
  }

  @Test
  fun `onCompilationUnit can override schemaFile for Android Projects`() {
    withTestProject("compilationUnitAndroid") { dir ->
      TestUtils.executeTaskAndAssertSuccess(":app:generateApolloSources", dir)
    }
  }

  @Test
  fun `onCompilationUnits for main sourceSet should not generate for test`() {
    withSimpleProject("""
      apollo {
        onCompilationUnit {
          if (variantName == "main") {
            schemaFile = file("src/main/graphql/com/example/schema.json")
            graphqlSourceDirectorySet.srcDir(file("src/main/graphql/"))
            graphqlSourceDirectorySet.include("**/*.graphql")
          }
        }
      }
    """.trimIndent()) { dir ->
      TestUtils.executeTask("generateApolloSources", dir)
      assertTrue(dir.generatedChild("main/service/com/example/DroidDetailsQuery.kt").exists())
      assertTrue(dir.generatedChild("test/service/com/example/DroidDetailsQuery.kt").exists().not())
    }
  }

  @Test
  fun `explicit schemaFile and graphqlSourceDirectorySet should not generate for test`() {
    withSimpleProject("""
      apollo {
        schemaFile = file("src/main/graphql/com/example/schema.json")
        graphqlSourceDirectorySet.srcDir(file("src/main/graphql/"))
        graphqlSourceDirectorySet.include("**/*.graphql")
      }
    """.trimIndent()) { dir ->
      TestUtils.executeTask("generateApolloSources", dir)
      assertTrue(dir.generatedChild("main/service/com/example/DroidDetailsQuery.kt").exists())
      assertTrue(dir.generatedChild("test/service/com/example/DroidDetailsQuery.kt").exists().not())
    }
  }

  @Test
  fun `graphql queries under test sources should still generate for test`() {
    withSimpleProject("""
      apollo {
        schemaFile = file("src/main/graphql/com/example/schema.json")
      }
    """.trimIndent()) { dir ->
      // Move AllFilms into test sources
      dir.child("src/main/graphql/com/example/AllFilms.graphql").delete()
      fixturesDirectory().child("starwars/AllFilms.graphql").copyTo(target = dir.child("src/test/graphql/com/example/AllFilms.graphql"))

      TestUtils.executeTask("generateApolloSources", dir)
      assertTrue(dir.generatedChild("test/service/com/example/FilmsQuery.kt").exists())
    }
  }

  @Test
  fun `explicit schemaFile outside the module should not generate for test`() {
    withSimpleProject("""
      apollo {
        schemaFile = file("../schema.json")
      }
    """.trimIndent()) { dir ->
      val dest = File(dir, "../schema.json")
      File(dir, "src/main/graphql/com/example/schema.json").copyTo(dest, true)
      TestUtils.executeTask("generateApolloSources", dir)
      assertTrue(dir.generatedChild("main/service/testProject/src/main/graphql/com/example/DroidDetailsQuery.kt").exists())
      assertTrue(dir.generatedChild("test/service/").exists().not())
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
  fun `generateOperationOutput generates queries with __typename`() {
    withSimpleProject("""
      apollo {
        generateOperationOutput = true
      }
    """.trimIndent()) { dir ->
      val result = TestUtils.executeTask("generateApolloSources", dir)

      assertEquals(TaskOutcome.SUCCESS, result.task(":generateApolloSources")!!.outcome)
      val operationOutput = dir.child("build/generated/operationOutput/apollo/main/service/OperationOutput.json")

      // Check that the filename case did not change. See https://github.com/apollographql/apollo-android/issues/2533
      assertTrue(operationOutput.canonicalFile.path.endsWith("build/generated/operationOutput/apollo/main/service/operationOutput.json"))

      assertThat(operationOutput.readText(), containsString("__typename"))
    }
  }

  @Test
  fun `generateOperationOutput uses same id as the query`() {
    withSimpleProject("""
      apollo {
        generateOperationOutput = true
      }
    """.trimIndent()) { dir ->
      val result = TestUtils.executeTask("generateApolloSources", dir)

      assertEquals(TaskOutcome.SUCCESS, result.task(":generateApolloSources")!!.outcome)
      val expectedOperationId = "260dd8d889c94e78b975e435300929027d0ad10ea55b63695b13894eb8cd8578"
      val operationOutput = dir.child("build/generated/operationOutput/apollo/main/service/operationOutput.json")
      assertThat(operationOutput.readText(), containsString(expectedOperationId))

      val queryJavaFile = dir.generatedChild("main/service/com/example/DroidDetailsQuery.kt")
      assertThat(queryJavaFile.readText(), containsString(expectedOperationId))
    }
  }

  @Test
  fun `compilation unit directory properties carry task dependencies`() {
    withSimpleProject("""
      apollo {
        generateOperationOutput = true
        
        onCompilationUnit { compilationUnit ->
          tasks.register("customTask" + compilationUnit.name.capitalize()) {
            inputs.file(compilationUnit.operationOutputFile)
          }
        }
      }
    """.trimIndent()) { dir ->
      val result = TestUtils.executeTask("customTaskMainService", dir)

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

      assertTrue(dir.generatedChild("main/service/com/example/fragment/SpeciesInformation.kt").isFile)
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

      assertTrue(dir.generatedChild("main/service/example/fragment/SpeciesInformation.kt").isFile)
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
