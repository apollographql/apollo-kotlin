package com.apollographql.apollo.gradle.test

import util.TestUtils
import org.junit.Test

class JavaOnly {
  @Test
  fun javaOnlyProject() {
    TestUtils.withTestProject("java-only") { dir ->
      TestUtils.executeTaskAndAssertSuccess(":generateApolloSources", dir)
    }
  }
}