package com.apollographql.apollo.gradle.test

import com.apollographql.apollo.gradle.util.TestUtils
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Test

class MultiModulesTests {
  @Test
  fun `multi-modules project compiles`() {
    TestUtils.withTestProject("multi-modules") { dir ->
      val result = TestUtils.executeTask(":leaf:assemble", dir)
      Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":leaf:assemble")!!.outcome)
    }
  }

  @Test
  fun `multi-modules project can use transitive dependencies`() {
    TestUtils.withTestProject("multi-modules-transitive") { dir ->
      val result = TestUtils.executeTask(":leaf:generateApolloSources", dir)
      Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":leaf:generateApolloSources")!!.outcome)
    }
  }

  @Test
  fun `transitive dependencies are only included once`() {
    /**
     * A dimaond shaped hierarchy does not include the schema multiple times
     */
    TestUtils.withTestProject("multi-modules-duplicate") { dir ->
      val result = TestUtils.executeTask(":leaf:generateApolloSources", dir)
      Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":leaf:generateApolloSources")!!.outcome)
    }
  }
}