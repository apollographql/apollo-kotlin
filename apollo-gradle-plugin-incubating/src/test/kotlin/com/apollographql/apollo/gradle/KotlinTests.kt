package com.apollographql.apollo.gradle

import com.apollographql.apollo.gradle.util.TestUtils
import org.junit.Test

class KotlinTests {
  @Test
  fun `plugin applies and generates kotlin files`() {
    TestUtils.withGradleRunner("kotlin") { _, runner ->
      runner.withArguments("build", "--stacktrace").build()
    }
  }
}