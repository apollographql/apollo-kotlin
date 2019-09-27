package com.apollographql.apollo.gradle.test

import com.apollographql.apollo.compiler.child
import com.apollographql.apollo.gradle.util.TestUtils
import com.apollographql.apollo.gradle.util.TestUtils.withProject
import com.apollographql.apollo.gradle.util.generatedChild
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MultipleServicesTests {
  fun withMultipleServicesProject(apolloConfiguration: String, block: (File) -> Unit) {
    withProject(usesKotlinDsl = false,
        plugins = listOf(TestUtils.javaPlugin, TestUtils.apolloPlugin),
        apolloConfiguration = apolloConfiguration) { dir ->
      val source = TestUtils.fixturesDirectory()
      val target = dir.child("src", "main", "graphql", "githunt")
      File(source, "githunt").copyRecursively(target = target, overwrite = true)

      block(dir)
    }
  }

  @Test
  fun `default services are found`() {
    withMultipleServicesProject("") { dir ->
      TestUtils.executeTask("build", dir)

      assertTrue(dir.generatedChild("main/service0/com/example/DroidDetailsQuery.java").isFile)
      assertTrue(dir.generatedChild("main/service0/com/example/FilmsQuery.java").isFile)
      assertTrue(dir.generatedChild("main/service0/com/example/fragment/SpeciesInformation.java").isFile)
      assertTrue(dir.generatedChild("main/service1/githunt/FeedQuery.java").isFile)
      assertTrue(dir.generatedChild("main/service1/githunt/fragment/RepositoryFragment.java").isFile)
    }
  }

  @Test
  fun `can specify services explicitely`() {
    val apolloConfiguration = """
      apollo {
        service("starwars") {
          schemaFilePath = "src/main/graphql/com/example/schema.json"
        }
        service("githunt") {
          schemaFilePath = "src/main/graphql/githunt/schema.json"
        }
      }
    """.trimIndent()
    withMultipleServicesProject(apolloConfiguration) { dir ->
      TestUtils.executeTask("build", dir)

      assertTrue(dir.generatedChild("main/starwars/com/example/DroidDetailsQuery.java").isFile)
      assertTrue(dir.generatedChild("main/starwars/com/example/FilmsQuery.java").isFile)
      assertTrue(dir.generatedChild("main/starwars/com/example/fragment/SpeciesInformation.java").isFile)
      assertTrue(dir.generatedChild("main/githunt/githunt/FeedQuery.java").isFile)
      assertTrue(dir.generatedChild("main/githunt/githunt/fragment/RepositoryFragment.java").isFile)
    }
  }
}