package com.apollographql.apollo3.gradle.test

import com.apollographql.apollo3.gradle.util.TestUtils
import org.junit.Test

class JavaOnly {
  @Test
  fun javaOnlyProject() {
    TestUtils.withTestProject("java-only") { dir ->
      TestUtils.executeTaskAndAssertSuccess(":generateApolloSources", dir)
    }
  }
}