package com.apollographql.apollo3.gradle.test

import com.apollographql.apollo3.gradle.util.TestUtils
import com.apollographql.apollo3.gradle.util.TestUtils.withTestProject
import org.junit.Test

class ConfigurationCacheTests {
  @Test
  fun configurationCacheTest() = withTestProject("configuration-cache") { dir ->
    val buildResult = TestUtils.executeGradle(
        dir,
        "--configuration-cache",
        "generateApolloSources"
    )
  }
}