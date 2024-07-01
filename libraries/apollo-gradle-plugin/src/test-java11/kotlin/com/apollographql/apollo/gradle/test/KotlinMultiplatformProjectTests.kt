package com.apollographql.apollo.gradle.test

import util.TestUtils
import org.junit.Test

class KotlinMultiplatformProjectTests {
  @Test
  fun `ios framework compiles`() {
    TestUtils.withTestProject("multiplatform") { dir ->
      TestUtils.executeTaskAndAssertSuccess(":linkDebugFrameworkIosArm64", dir)
    }
  }
}