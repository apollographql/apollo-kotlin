package com.apollographql.apollo.gradle.test


import com.apollographql.apollo.gradle.util.TestUtils
import com.apollographql.apollo.gradle.util.TestUtils.withSimpleProject
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class DownloadSchemaTests {
  val mockServer = MockWebServer()

  val apolloConfiguration = """
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
      val content = "schema should be here"
      val mockResponse = MockResponse().setBody(content)
      mockServer.enqueue(mockResponse)

      TestUtils.executeTask("downloadMockApolloSchemaFromIntrospection", dir)

      assertEquals(content, File(dir, "src/main/graphql/com/example/schema.json").readText())
    }
  }

  @Test
  fun `download schema is never up-to-date`() {

    withSimpleProject(apolloConfiguration = apolloConfiguration) { dir ->
      val content = "schema should be here"
      val mockResponse = MockResponse().setBody(content)
      mockServer.enqueue(mockResponse)

      var result = TestUtils.executeTask("downloadMockApolloSchemaFromIntrospection", dir)
      assertEquals(TaskOutcome.SUCCESS, result.task(":downloadMockApolloSchemaFromIntrospection")?.outcome)

      mockServer.enqueue(mockResponse)

      // Since the task does not declare any output, it should never be up-to-date
      result = TestUtils.executeTask("downloadMockApolloSchemaFromIntrospection", dir)
      assertEquals(TaskOutcome.SUCCESS, result.task(":downloadMockApolloSchemaFromIntrospection")?.outcome)

      assertEquals(content, File(dir, "src/main/graphql/com/example/schema.json").readText())
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

      val content1 = "schema1"
      mockServer.enqueue(MockResponse().setBody(content1))

      TestUtils.executeTask("downloadMockApolloSchemaFromIntrospection", dir, "--build-cache")
      assertEquals(content1, schemaFile.readText())

      val content2 = "schema2"
      mockServer.enqueue(MockResponse().setBody(content2))

      TestUtils.executeTask("downloadMockApolloSchemaFromIntrospection", dir, "--build-cache")
      assertEquals(content2, schemaFile.readText())
    }
  }

  @Test
  fun `manually downloading a schema is working`() {

    withSimpleProject(apolloConfiguration = "") { dir ->
      val content = "schema should be here"
      val mockResponse = MockResponse().setBody(content)
      mockServer.enqueue(mockResponse)

      // Tests are running with a working dir of "apollo-gradle-plugin", don't pollute that folder
      val schema = File("build/testProject/schema.json")

      TestUtils.executeGradle(dir, "downloadApolloSchema",
          "--schema=${schema.path}",
          "--endpoint=${mockServer.url("/")}")

      assertEquals(content, schema.readText())
    }
  }
}
