import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion

fun Project.configureMppDefaults(withJs: Boolean = true, withLinux: Boolean = true) {
  val kotlinExtension = extensions.findByName("kotlin") as? KotlinMultiplatformExtension
  check(kotlinExtension != null) {
    "No multiplatform extension found"
  }
  kotlinExtension.apply {
    /**
     * configure targets
     */
    jvm()

    if (withJs) {
      js(BOTH) {
        nodejs {
          testTask {
            useMocha {
              // Override default 2s timeout
              timeout = "120s"
            }
          }
        }
      }
    }

    if (withLinux) {
      linuxX64("linux")
    }

    configureAppleTargets(
        "macosX64",
        "macosArm64",
        "iosArm64",
        "iosX64",
        "iosSimulatorArm64",
        "watchosArm64",
        "watchosSimulatorArm64",
        "tvosArm64",
        "tvosX64",
        "tvosSimulatorArm64",
    )

    addTestDependencies(withJs)
  }
}

fun Project.okio(): String {
  val okioVersion = when (getKotlinPluginVersion()) {
    "1.6.10" -> "3.0.0"
    else -> "3.1.0"
  }

  return "${groovy.util.Eval.x(project, "x.dep.okio")}:$okioVersion"
}

fun Project.okioNodeJs(): String {
  val okioVersion = when (getKotlinPluginVersion()) {
    "1.6.10" -> "3.0.0"
    else -> "3.1.0"
  }

  return "${groovy.util.Eval.x(project, "x.dep.okioNodeJs")}:$okioVersion"
}

fun KotlinMultiplatformExtension.configureAppleTargets(vararg presetNames: String) {
  if (System.getProperty("idea.sync.active") != null) {
    // Early return. Inside intelliJ, only configure one target
    // Try to guess the dev machine to make sure the tests are running smoothly
    if (System.getProperty("os.arch") == "aarch64") {
      macosArm64("apple")
    } else {
      macosX64("apple")
    }

    return
  }
  val appleMain = sourceSets.create("appleMain")
  val appleTest = sourceSets.create("appleTest")

  appleMain.dependsOn(sourceSets.getByName("commonMain"))
  appleTest.dependsOn(sourceSets.getByName("commonTest"))

  presetNames.forEach { presetName ->
    targetFromPreset(
        presets.getByName(presetName),
        presetName,
    )

    sourceSets.getByName("${presetName}Main").dependsOn(appleMain)
    sourceSets.getByName("${presetName}Test").dependsOn(appleTest)
  }
}

/**
 * Same as [configureMppDefaults] but without iOS or Linux targets.
 * Tests only run on the JVM, JS and MacOS
 */
fun Project.configureMppTestsDefaults(withJs: Boolean = true) {
  val kotlinExtension = extensions.findByName("kotlin") as? KotlinMultiplatformExtension
  check(kotlinExtension != null) {
    "No multiplatform extension found"
  }
  kotlinExtension.apply {
    /**
     * configure targets
     */
    jvm()
    if (withJs) {
      js(IR) {
        nodejs {
          testTask {
            useMocha {
              // Override default 2s timeout
              timeout = "120s"
            }
          }
        }
      }
    }

    configureAppleTargets("macosX64", "macosArm64")

    addTestDependencies(withJs)
  }
}

fun KotlinMultiplatformExtension.addTestDependencies(withJs: Boolean) {
  sourceSets.getByName("commonTest") {
    it.dependencies {
      implementation(kotlin("test-common"))
      implementation(kotlin("test-annotations-common"))
    }
  }
  if (withJs) {
    sourceSets.getByName("jsTest") {
      it.dependencies {
        implementation(kotlin("test-js"))
      }
    }
  }
  sourceSets.getByName("jvmTest") {
    it.dependencies {
      implementation(kotlin("test-junit"))
    }
  }
}
