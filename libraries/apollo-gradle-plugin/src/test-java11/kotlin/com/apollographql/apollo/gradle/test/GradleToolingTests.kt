package com.apollographql.apollo.gradle.test

import com.apollographql.apollo.gradle.api.ApolloGradleToolingModel
import org.gradle.tooling.GradleConnector
import org.junit.Assert
import org.junit.Test
import util.TestUtils
import java.io.File

class GradleToolingTests {
  private fun withMultipleServicesProject(apolloConfiguration: String, block: (File) -> Unit) {
    TestUtils.withProject(
        usesKotlinDsl = false,
        plugins = listOf(TestUtils.kotlinJvmPlugin, TestUtils.apolloPlugin),
        apolloConfiguration = apolloConfiguration
    ) { dir ->
      val source = TestUtils.fixturesDirectory()

      val target = File(dir, "src/main/graphql/githunt")
      File(source, "githunt").copyRecursively(target = target, overwrite = true)

      File(dir, "src/main/graphql/com").copyRecursively(target = File(dir, "src/main/graphql/starwars"), overwrite = true)
      File(dir, "src/main/graphql/com").deleteRecursively()

      block(dir)
    }
  }

  @Test
  fun `tooling model exposes all service infos`() {
    val apolloConfiguration = """
      apollo {
        service("starwars") {
          packageName.set("starwars")
          srcDir("src/main/graphql/starwars")
          introspection {
            endpointUrl.set("https://example.com")
            schemaFile.set(file("schema.graphqls"))
            headers.set(["header1": "value1"])
          }
        }
        service("githunt") {
          packageName.set("githunt")
          srcDir("src/main/graphql/githunt")
        }
      }
    """.trimIndent()
    withMultipleServicesProject(apolloConfiguration) { dir ->
      val toolingModel = GradleConnector.newConnector()
          .forProjectDirectory(dir)
          .connect()
          .use { connection ->
            connection.getModel(ApolloGradleToolingModel::class.java)
          }
      Assert.assertEquals(ApolloGradleToolingModel.VERSION_MAJOR, toolingModel.versionMajor)
      Assert.assertEquals(4, toolingModel.versionMinor)
      @Suppress("DEPRECATION")
      Assert.assertEquals(emptyList<String>(), toolingModel.serviceInfos.flatMap { it.upstreamProjects })
      Assert.assertEquals(emptyList<String>(), toolingModel.serviceInfos.flatMap { it.upstreamProjectPaths })

      val serviceInfo0 = toolingModel.serviceInfos[0]
      Assert.assertEquals("starwars", serviceInfo0.name)
      Assert.assertEquals(setOf(File(dir, "src/main/graphql/starwars")), serviceInfo0.graphqlSrcDirs)
      Assert.assertEquals(setOf(File(dir, "src/main/graphql/starwars/example/schema.json")), serviceInfo0.schemaFiles)
      Assert.assertEquals("https://example.com", serviceInfo0.endpointUrl)
      Assert.assertEquals(mapOf("header1" to "value1"), serviceInfo0.endpointHeaders)

      val serviceInfo1 = toolingModel.serviceInfos[1]
      Assert.assertEquals("githunt", serviceInfo1.name)
      Assert.assertEquals(setOf(File(dir, "src/main/graphql/githunt")), serviceInfo1.graphqlSrcDirs)
      Assert.assertEquals(setOf(File(dir, "src/main/graphql/githunt/schema.json")), serviceInfo1.schemaFiles)
    }
  }

  @Test
  fun `tooling model exposes apollo metadata dependencies`() {
    TestUtils.withTestProject("multi-modules-diamond") { dir ->
      val toolingModel = GradleConnector.newConnector()
          .forProjectDirectory(File(dir, "leaf"))
          .connect()
          .use { connection ->
            connection.getModel(ApolloGradleToolingModel::class.java)
          }
      Assert.assertEquals(ApolloGradleToolingModel.VERSION_MAJOR, toolingModel.versionMajor)
      Assert.assertEquals(4, toolingModel.versionMinor)
      @Suppress("DEPRECATION")
      Assert.assertEquals(listOf("node1", "node2"), toolingModel.serviceInfos.flatMap { it.upstreamProjects }.sorted())
      Assert.assertEquals(listOf(":node1", ":node2"), toolingModel.serviceInfos.flatMap { it.upstreamProjectPaths }.sorted())
    }
  }
}
