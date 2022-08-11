package com.apollographql.apollo3.buildlogic

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

fun Project.configureMppDefaults(withJs: Boolean, withLinux: Boolean) {
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

    configureAppleTargets(setOf(
        "macosX64",
        "macosArm64",
        "iosArm64",
        "iosX64",
        "iosSimulatorArm64",
        "watchosArm32",
        "watchosArm64",
        "watchosSimulatorArm64",
        "tvosArm64",
        "tvosX64",
        "tvosSimulatorArm64",
    ))

    addTestDependencies()

    enableNewMemoryManager()
  }
}

private fun KotlinMultiplatformExtension.configureAppleTargets(presetNames: Set<String>) {
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
 * Same as [configureMppDefaults] but without Linux targets. Apple targets can be configured.
 * Tests only run on the JVM, JS and MacOS
 */
fun Project.configureMppTestsDefaults(
    withJs: Boolean,
    withJvm: Boolean,
    newMemoryManager: Boolean,
    appleTargets: Set<String>,
) {
  val kotlinExtension = extensions.findByName("kotlin") as? KotlinMultiplatformExtension
  check(kotlinExtension != null) {
    "No multiplatform extension found"
  }
  kotlinExtension.apply {
    /**
     * configure targets
     */
    if (withJvm) jvm()

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

    configureAppleTargets(appleTargets)

    addTestDependencies()

    if (newMemoryManager) enableNewMemoryManager()
  }
}

private fun KotlinMultiplatformExtension.addTestDependencies() {
  sourceSets.getByName("commonTest") {
    dependencies {
      implementation(kotlin("test"))
    }
  }
}

// See https://github.com/JetBrains/kotlin/blob/master/kotlin-native/NEW_MM.md
private fun KotlinMultiplatformExtension.enableNewMemoryManager() {
  targets.withType(KotlinNativeTarget::class.java) {
    binaries.all {
      binaryOptions["memoryModel"] = "experimental"
    }
  }
}
