package com.apollographql.apollo.gradle.test

import com.apollographql.apollo.gradle.util.TestUtils
import org.junit.Test

class PluginIdsTests {

  @Test
  fun `com-apollographql-apollo-kotlin compiles and adds the runtime dependency`() {
    TestUtils.withTestProject("apollo-kotlin") { dir ->
      TestUtils.executeTaskAndAssertSuccess(":build", dir)
    }
  }

  @Test
  fun `com-apollographql-apollo-java compiles and adds the runtime dependency`() {
    TestUtils.withTestProject("apollo-java") { dir ->
      TestUtils.executeTaskAndAssertSuccess(":build", dir)
    }
  }
}