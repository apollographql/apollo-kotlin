package test

import util.TestUtils
import com.google.common.truth.Truth
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test
import util.disableIsolatedProjects

class KotlinPluginVersionTests {

  /**
   * Using the minimum supported versions of Kotlin for JVM should work.
   */
  @Test
  fun `kotlin JVM min version succeeds`() {
    TestUtils.withTestProject("kotlin-plugin-version-min") { dir ->
      dir.disableIsolatedProjects() // old KGP versions do not support isolated projects

      val result = TestUtils.executeGradleWithVersion(dir, "8.10", "build")

      Truth.assertThat(result.task(":build")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }
  }
}
