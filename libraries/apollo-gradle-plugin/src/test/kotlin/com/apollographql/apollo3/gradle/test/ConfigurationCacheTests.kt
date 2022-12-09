package com.apollographql.apollo3.gradle.test

import com.apollographql.apollo3.gradle.util.TestUtils
import com.apollographql.apollo3.gradle.util.TestUtils.withTestProject
import com.apollographql.apollo3.gradle.util.replaceInText
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Test

class ConfigurationCacheTests {
  @Test
  fun configurationCacheTest() = withTestProject("configuration-cache") { dir ->
    val server = MockWebServer()

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

    server.enqueue(MockResponse().setBody(minimalValidSchema))
    TestUtils.executeGradle(
        dir,
        "--configuration-cache",
        "generateApolloSources",
        "downloadServiceApolloSchemaFromIntrospection"
    )

    server.enqueue(MockResponse().setBody(minimalValidSchema))
    val result = TestUtils.executeGradle(
        dir,
        "--configuration-cache",
        "generateApolloSources",
        "downloadServiceApolloSchemaFromIntrospection"
    )
    assert(result.output.contains("Reusing configuration cache."))
  }
}
