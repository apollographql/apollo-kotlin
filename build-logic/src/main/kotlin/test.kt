import com.gradle.develocity.agent.gradle.test.DevelocityTestConfiguration
import org.gradle.api.Project
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.ExecutionTaskHolder
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithSimulatorTests
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithTests
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeOutputKind
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget

fun Project.configureTesting() {
  tasks.withType(Test::class.java) {
    it.forwardEnv("updateTestFixtures")
    it.forwardEnv("testFilter")
    it.forwardEnv("codegenModels")
  }

  pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
    tasks.withType(Test::class.java) {
      it.extensions.getByType(DevelocityTestConfiguration::class.java).testRetry {
        if (isCIBuild()) {
          it.maxRetries.set(3)
          it.failOnPassedAfterRetry.set(true)
        }
      }
    }
  }

  tasks.withType(AbstractTestTask::class.java) {
    it.testLogging {
      it.exceptionFormat = TestExceptionFormat.FULL
      it.events.add(TestLogEvent.PASSED)
      it.events.add(TestLogEvent.FAILED)
      it.showStandardStreams = true
    }
  }

  addTestDependencies()
}

private fun Project.addTestDependencies() {
  kotlinExtensionOrNull?.apply {
    when (this) {
      is KotlinMultiplatformExtension -> {
        sourceSets.getByName("commonTest") {
          it.dependencies {
            implementation("org.jetbrains.kotlin:kotlin-test:${getKotlinPluginVersion()}")
          }
        }
        sourceSets.findByName("androidInstrumentedTest")?.apply {
          dependencies {
            implementation("org.jetbrains.kotlin:kotlin-test:${getKotlinPluginVersion()}")
            implementation(getCatalogLib("androidx.test.runner"))
          }
        }
      }

      is KotlinJvmProjectExtension -> {
        dependencies.add("testImplementation", "org.jetbrains.kotlin:kotlin-test:${getKotlinPluginVersion()}")
      }
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

internal fun Project.disableSomeTests(enableWasmJsTests: Boolean) {
  project.kotlinTargets.forEach { target ->
    /**
     * Disable every native test except the KotlinNativeTargetWithHostTests to save some time
     */
    if (target is KotlinNativeTargetWithSimulatorTests || target is KotlinNativeTargetWithTests<*>) {
      target.testRuns.configureEach {
        it as ExecutionTaskHolder<*>
        it.executionTask.configure {
          it.enabled = false
        }
      }
      target.binaries.configureEach {
        if (it.outputKind == NativeOutputKind.TEST) {
          it.linkTaskProvider.configure {
            it.enabled = false
          }
          it.compilation.compileTaskProvider.configure {
            it.enabled = false
          }
        }
      }
    }
    /**
     * Disable wasmJs tests because they are not ready yet
     */
    if (!enableWasmJsTests && target is KotlinJsIrTarget && target.wasmTargetType != null) {
      target.subTargets.configureEach {
        it.testRuns.configureEach {
          it.executionTask.configure {
            it.enabled = false
          }
        }
      }
      target.testRuns.configureEach {
        it.executionTask.configure {
          it.enabled = false
        }
      }
      target.binaries.configureEach {
        it.compilation.compileTaskProvider.configure {
          it.enabled = false
        }
      }
    }
  }
}