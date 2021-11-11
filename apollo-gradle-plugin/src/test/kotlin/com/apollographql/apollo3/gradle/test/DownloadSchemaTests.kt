package com.apollographql.apollo3.gradle.test


import com.apollographql.apollo3.gradle.util.TestUtils
import com.apollographql.apollo3.gradle.util.TestUtils.withSimpleProject
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class DownloadSchemaTests {
  private val mockServer = MockWebServer()
  private val schemaString1 = """
        {
          "__schema": {
            "queryType": {
              "name": "foo"
            },
            "types": []
          }
        }
      """.trimIndent()

  private val schemaString2 = schemaString1.replace("foo", "bar")

  private val apolloConfiguration = """
      apollo {
        service("mock") {
          schemaFile = file("src/main/graphql/com/example/schema.json")
          introspection {
            endpointUrl = "${mockServer.url("/").toUrl()}"
          }
        }
      }
    """.trimIndent()

  @Test
  fun `schema is downloaded correctly`() {
    withSimpleProject(apolloConfiguration = apolloConfiguration) { dir ->
      val mockResponse = MockResponse().setBody(schemaString1)
      mockServer.enqueue(mockResponse)

      TestUtils.executeTask("downloadMockApolloSchemaFromIntrospection", dir)

      assertEquals(schemaString1, File(dir, "src/main/graphql/com/example/schema.json").readText())
    }
  }

  @Test
  fun `download schema is never up-to-date`() {

    withSimpleProject(apolloConfiguration = apolloConfiguration) { dir ->
      val mockResponse = MockResponse().setBody(schemaString1)
      mockServer.enqueue(mockResponse)

      var result = TestUtils.executeTask("downloadMockApolloSchemaFromIntrospection", dir)
      assertEquals(TaskOutcome.SUCCESS, result.task(":downloadMockApolloSchemaFromIntrospection")?.outcome)

      mockServer.enqueue(mockResponse)

      // Since the task does not declare any output, it should never be up-to-date
      result = TestUtils.executeTask("downloadMockApolloSchemaFromIntrospection", dir)
      assertEquals(TaskOutcome.SUCCESS, result.task(":downloadMockApolloSchemaFromIntrospection")?.outcome)

      assertEquals(schemaString1, File(dir, "src/main/graphql/com/example/schema.json").readText())
    }
  }

  @Test
  fun `download schema is never cached`() {

    withSimpleProject(apolloConfiguration = apolloConfiguration) { dir ->
      val buildCacheDir = File(dir, "buildCache")

      File(dir, "settings.gradle").appendText(""" 
        
        // the empty line above is important
        buildCache {
            local {
                directory '${buildCacheDir.absolutePath}'
            }
        }
      """.trimIndent())

      val schemaFile = File(dir, "src/main/graphql/com/example/schema.json")

      mockServer.enqueue(MockResponse().setBody(schemaString1))

      TestUtils.executeTask("downloadMockApolloSchemaFromIntrospection", dir, "--build-cache")
      assertEquals(schemaString1, schemaFile.readText())

      mockServer.enqueue(MockResponse().setBody(schemaString2))

      TestUtils.executeTask("downloadMockApolloSchemaFromIntrospection", dir, "--build-cache")
      assertEquals(schemaString2, schemaFile.readText())
    }
  }

  @Test
  fun `manually downloading a schema is working`() {

    withSimpleProject(apolloConfiguration = "") { dir ->
      val mockResponse = MockResponse().setBody(schemaString1)
      mockServer.enqueue(mockResponse)

      // Tests can run from any working directory.
      // They used to run in `apollo-gradle-plugin` but with Gradle 6.7, they now run in something like
      // /private/var/folders/zh/xlpqxsfn7vx_dhjswsgsps6h0000gp/T/.gradle-test-kit-martin/test-kit-daemon/6.7/
      // We'll use absolute path as arguments for the check to succeed later on
      val schema = File("build/testProject/schema.json")

      TestUtils.executeGradle(dir, "downloadApolloSchema",
          "--schema=${schema.absolutePath}",
          "--endpoint=${mockServer.url("/")}")

      assertEquals(schemaString1, schema.readText())
    }
  }
}
