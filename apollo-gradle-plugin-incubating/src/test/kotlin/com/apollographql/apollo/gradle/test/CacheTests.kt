package com.apollographql.apollo.gradle.test

import com.apollographql.apollo.gradle.internal.child
import com.apollographql.apollo.gradle.util.TestUtils
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Test
import java.io.File

class CacheTests {

  @Test
  fun `generate apollo classes task is cached`() {
    TestUtils.withSimpleProject { dir ->

      val buildCacheDir = dir.child("buildCache")

      File(dir, "settings.gradle").appendText("""
        
        buildCache {
            local {
                directory '${buildCacheDir.absolutePath}'
            }
        }
      """.trimIndent())

      System.out.println("build the project")
      var result = TestUtils.executeTask("generateMainServiceApolloSources", dir, "--build-cache")

      Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":generateMainServiceApolloSources")!!.outcome)

      System.out.println("delete build folder")
      dir.child("build").deleteRecursively()

      System.out.println("build from cache")
      result = TestUtils.executeTask("generateMainServiceApolloSources", dir, "--build-cache")

      Assert.assertEquals(TaskOutcome.FROM_CACHE, result.task(":generateMainServiceApolloSources")!!.outcome)
    }
  }
}
