package com.apollographql.apollo.gradle.test

import com.apollographql.apollo.gradle.util.TestUtils
import com.apollographql.apollo.gradle.util.TestUtils.withProject
import com.apollographql.apollo.gradle.util.generatedChild
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidTests {

  @Test
  fun `android library compiles`() {
    withProject(apolloConfiguration = "",
        usesKotlinDsl = false,
        plugins = listOf(TestUtils.androidLibraryPlugin, TestUtils.apolloPlugin, TestUtils.kotlinAndroidPlugin)) { dir ->
      val result = TestUtils.executeTask("build", dir)

      assertEquals(TaskOutcome.SUCCESS, result.task(":build")!!.outcome)

      // Java classes generated successfully
      assertTrue(dir.generatedChild("service/com/example/DroidDetailsQuery.kt").isFile)
      assertTrue(dir.generatedChild("service/com/example/FilmsQuery.kt").isFile)
      assertTrue(dir.generatedChild("service/com/example/fragment/SpeciesInformation.kt").isFile)
    }
  }

  @Test
  fun `android application compiles`() {
    withProject(apolloConfiguration = "",
        usesKotlinDsl = false,
        plugins = listOf(TestUtils.androidApplicationPlugin, TestUtils.apolloPlugin, TestUtils.kotlinAndroidPlugin)) { dir ->
      val result = TestUtils.executeTask("build", dir)

      assertEquals(TaskOutcome.SUCCESS, result.task(":build")!!.outcome)

      // Java classes generated successfully
      assertTrue(dir.generatedChild("service/com/example/DroidDetailsQuery.kt").isFile)
      assertTrue(dir.generatedChild("service/com/example/FilmsQuery.kt").isFile)
      assertTrue(dir.generatedChild("service/com/example/fragment/SpeciesInformation.kt").isFile)
    }
  }

  @Test
  fun `product flavors compiles`() {
    withProject(apolloConfiguration = "",
        usesKotlinDsl = false,
        isFlavored = true,
        plugins = listOf(TestUtils.androidLibraryPlugin, TestUtils.apolloPlugin, TestUtils.kotlinAndroidPlugin)) { dir ->

      TestUtils.executeTask("build", dir)

      assertTrue(dir.generatedChild("service/com/example/DroidDetailsQuery.kt").isFile)
    }
  }
}
