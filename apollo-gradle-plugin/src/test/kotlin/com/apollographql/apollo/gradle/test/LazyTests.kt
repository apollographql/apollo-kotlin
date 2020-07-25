package com.apollographql.apollo.gradle.test

import com.apollographql.apollo.gradle.util.TestUtils
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Test


class LazyTests {
  @Test
  fun `properties are not called during configuration`() {

    val apolloConfiguration = """
configure<ApolloExtension> {
  useSemanticNaming.set(project.provider {
      throw IllegalArgumentException("this should not be called during configuration")
  })
}
    """.trimIndent()
    TestUtils.withProject(
        usesKotlinDsl = true,
        plugins = listOf(TestUtils.javaPlugin, TestUtils.apolloPlugin),
        apolloConfiguration = apolloConfiguration
    ) { dir ->
      TestUtils.executeGradle(dir, "tasks", "--all")
    }
  }
}
