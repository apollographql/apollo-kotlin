package com.apollographql.apollo3.gradle.test

import com.apollographql.apollo3.gradle.util.TestUtils
import org.junit.Test

class GroovyTests {
  @Test
  fun groovyGradleProject() {
    TestUtils.withTestProject("groovy") { dir ->
      TestUtils.executeTaskAndAssertSuccess(":tasks", dir)
    }
  }
}
