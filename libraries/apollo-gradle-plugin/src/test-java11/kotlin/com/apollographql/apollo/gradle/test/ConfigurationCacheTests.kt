package com.apollographql.apollo.gradle.test

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Test
import util.TestUtils
import util.TestUtils.withTestProject
import util.replaceInText

class ConfigurationCacheTests {
  @Test
  fun configurationCacheTest() = withTestProject("configuration-cache") { dir ->
    val server = MockWebServer()

    val preIntrospectionResponse = """
      {
        "data": {
          "__schema": {
            "__typename": "__Schema",
            "types": []
          }
        }
      }
    """.trimIndent()

    val minimalValidSchema = """
        {
          "__schema": {
            "queryType": {
              "name": "foo"
            },
            "types": []
          }
        }
      """.trimIndent()

    dir.resolve("root/build.gradle.kts").replaceInText("ENDPOINT", server.url("/").toString())

    server.enqueue(MockResponse().setBody(preIntrospectionResponse))
    server.enqueue(MockResponse().setBody(minimalValidSchema))
    TestUtils.executeGradle(
        dir,
        "--configuration-cache",
        "generateApolloSources",
        "downloadServiceApolloSchemaFromIntrospection"
    )

    server.enqueue(MockResponse().setBody(preIntrospectionResponse))
    server.enqueue(MockResponse().setBody(minimalValidSchema))
    val result = TestUtils.executeGradle(
        dir,
        "--configuration-cache",
        "generateApolloSources",
        "downloadServiceApolloSchemaFromIntrospection"
    )
    assert(result.output.contains("Reusing configuration cache."))
  }

  @Test
  fun schemaCanBeRenamed() = withTestProject("configuration-cache2") { dir ->
    TestUtils.executeGradle(
        dir,
        "--configuration-cache",
        "generateApolloSources"
    )

    dir.resolve("src/main/graphql/schema.graphqls")
        .renameTo(dir.resolve("src/main/graphql/schema2.graphqls"))
    val result = TestUtils.executeGradle(
        dir,
        "--configuration-cache",
        "generateApolloSources",
    )
    assert(result.output.contains("Reusing configuration cache."))
  }
}
