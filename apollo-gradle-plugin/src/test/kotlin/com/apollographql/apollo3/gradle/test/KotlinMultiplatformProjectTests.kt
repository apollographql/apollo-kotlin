package com.apollographql.apollo3.gradle.test

import com.apollographql.apollo3.gradle.util.TestUtils
import org.junit.Test

class KotlinMultiplatformProjectTests {
  @Test
  fun `ios framework compiles`() {
    TestUtils.withTestProject("multiplatform") { dir ->
      TestUtils.executeTaskAndAssertSuccess(":linkDebugFrameworkIosArm64", dir)
    }
  }
}