package com.apollographql.apollo.gradle.test

import com.apollographql.apollo.gradle.internal.child
import com.apollographql.apollo.gradle.util.TestUtils
import com.apollographql.apollo.gradle.util.TestUtils.withSimpleProject
import com.apollographql.apollo.gradle.util.TestUtils.withTestProject
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
          schemaPath = "com/example/schema.json"
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

      TestUtils.executeTask("downloadMockApolloSchema", dir)

      assertEquals(content, dir.child("src", "main", "graphql", "com", "example", "schema.json").readText())
    }
  }

  @Test
  fun `download schema is never up-to-date`() {

    withSimpleProject(apolloConfiguration = apolloConfiguration) { dir ->
      val content = "schema should be here"
      val mockResponse = MockResponse().setBody(content)
      mockServer.enqueue(mockResponse)

      var result = TestUtils.executeTask("downloadMockApolloSchema", dir)
      assertEquals(TaskOutcome.SUCCESS, result.task(":downloadMockApolloSchema")?.outcome)

      mockServer.enqueue(mockResponse)

      // Since the task does not declare any output, it should never be up-to-date
      result = TestUtils.executeTask("downloadMockApolloSchema", dir)
      assertEquals(TaskOutcome.SUCCESS, result.task(":downloadMockApolloSchema")?.outcome)

      assertEquals(content, dir.child("src", "main", "graphql", "com", "example", "schema.json").readText())
    }
  }

  @Test
  fun `Android, downloadApolloSchema find the schema location`() {
    withTestProject("compilationUnitAndroid") { dir ->
      val content = "schema should be here"
      val mockResponse = MockResponse().setBody(content)
      mockServer.enqueue(mockResponse)

      TestUtils.executeTask("downloadApolloSchema", dir, "--endpoint=${mockServer.url("/").toUrl()}")

      assertEquals(content, dir.child( "schema.json").readText())
    }
  }

  @Test
  fun `Android, downloadApolloSchema endpoint and schema override the default resolution`() {
    withTestProject("compilationUnitAndroid") { dir ->
      val content = "schema should be here"
      val mockResponse = MockResponse().setBody(content)
      mockServer.enqueue(mockResponse)

      val schemaFile = File(dir, "schema.json")
      TestUtils.executeTask("downloadApolloSchema", dir, "--endpoint=${mockServer.url("/").toUrl()}", "--schema=${schemaFile.absolutePath}")

      assertEquals(content, schemaFile.readText())
    }
  }

  @Test
  fun `download schema is never cached`() {

    withSimpleProject(apolloConfiguration = apolloConfiguration) { dir ->
      val buildCacheDir = dir.child("buildCache")

      File(dir, "settings.gradle").appendText("""
        
        buildCache {
            local {
                directory '${buildCacheDir.absolutePath}'
            }
        }
      """.trimIndent())

      val schemaFile = dir.child("src", "main", "graphql", "com", "example", "schema.json")

      val content1 = "schema1"
      mockServer.enqueue(MockResponse().setBody(content1))

      TestUtils.executeTask("downloadMockApolloSchema", dir, "--build-cache")
      assertEquals(content1, schemaFile.readText())

      val content2 = "schema2"
      mockServer.enqueue(MockResponse().setBody(content2))

      TestUtils.executeTask("downloadMockApolloSchema", dir, "--build-cache")
      assertEquals(content2, schemaFile.readText())
    }
  }

  @Test
  fun `manually downloading a schema is working`() {

    withSimpleProject(apolloConfiguration = "") { dir ->
      val content = "schema should be here"
      val mockResponse = MockResponse().setBody(content)
      mockServer.enqueue(mockResponse)

      TestUtils.executeGradle(dir, "downloadApolloSchema",
          "-Pcom.apollographql.apollo.schema=schema.json",
          "-Pcom.apollographql.apollo.endpoint=${mockServer.url("/")}")

      assertEquals(content, dir.child("schema.json").readText())
    }
  }
}
