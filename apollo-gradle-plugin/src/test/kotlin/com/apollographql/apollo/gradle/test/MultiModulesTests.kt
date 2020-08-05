package com.apollographql.apollo.gradle.test

import com.apollographql.apollo.gradle.util.TestUtils
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Test

class MultiModulesTests {
  @Test
  fun `multi-modules project compiles`() {
    TestUtils.withTestProject("multi-modules") { dir ->

      val result = TestUtils.executeTask(":cli:assemble", dir)

      Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":cli:assemble")!!.outcome)
    }
  }
}