import com.gradle.enterprise.gradleplugin.testretry.retry
import org.gradle.api.Project
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

fun Project.configureTesting() {
  tasks.withType(Test::class.java) {
    forwardEnv("updateTestFixtures")
    forwardEnv("testFilter")
    forwardEnv("codegenModels")
  }
  
  pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
    tasks.withType(Test::class.java) {
      retry {
        if (isCIBuild()) {
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

/**
 * forwards an environment variable to a test task and mark it as input
 */
fun Test.forwardEnv(name: String) {
  System.getenv(name)?.let { value ->
    environment(name, value)
    inputs.property(name, value)
  }
}

// See https://github.com/gradle/gradle/issues/23456
fun Test.addRelativeInput(name: String, dirPath: Any) {
  this.inputs.dir(dirPath).withPropertyName(name).withPathSensitivity(PathSensitivity.RELATIVE)
}

fun isCIBuild() = !System.getenv("CI").isNullOrEmpty()