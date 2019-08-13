package com.apollographql.apollo.gradle

import com.apollographql.apollo.gradle.util.TestUtils.withGradleRunner
import org.junit.Test

class JavaTests {

  @Test
  fun `plugin applies and generates java file`() {
    withGradleRunner("java") { _, runner ->
      runner.withArguments("generateApolloClasses", "--stacktrace").build()
    }
  }
}