import com.gradle.enterprise.gradleplugin.testretry.retry
import org.gradle.api.Project
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

fun Project.configureTesting() {
  tasks.withType(Test::class.java) {
    systemProperty("updateTestFixtures", System.getProperty("updateTestFixtures"))
    systemProperty("testFilter", System.getProperty("testFilter"))
    systemProperty("codegenModels", System.getProperty("codegenModels"))

  }
  
  pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
    tasks.withType(Test::class.java) {
      retry {
        if (System.getenv().containsKey("CI")) {
          maxRetries.set(3)
          failOnPassedAfterRetry.set(true)
        }
      }
    }
  }

  tasks.withType(AbstractTestTask::class.java) {
    testLogging {
      exceptionFormat = TestExceptionFormat.FULL
      events.add(TestLogEvent.PASSED)
      events.add(TestLogEvent.FAILED)
      showStandardStreams = true
    }
  }
}
