import org.gradle.api.Action
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrLink
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink

private val allAppleTargets = setOf(
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
)

// Try to guess the dev machine to make sure the tests are running smoothly
val hostTarget: String
  get() = if (System.getProperty("os.arch") == "aarch64") {
    "macosArm64"
  } else {
    "macosX64"
  }

val enabledAppleTargets = allAppleTargets
val enabledLinux = true
val enabledJs = true

fun Project.configureMppDefaults(withJs: Boolean, withLinux: Boolean, withAndroid: Boolean) {
  configureMpp(
      withJvm = true,
      withJs = withJs,
      withLinux = withLinux,
      appleTargets = enabledAppleTargets,
      withAndroid = withAndroid,
  )
}


/**
 * Same as [configureMppDefaults] but without Linux targets. Apple targets can be configured.
 * Tests only run on the JVM, JS and macOS
 */
fun Project.configureMppTestsDefaults(
    withJs: Boolean,
    withJvm: Boolean,
    browserTest: Boolean,
    appleTargets: Collection<String>,
) {
  configureMpp(
      withJvm = withJvm,
      withJs = withJs,
      browserTest = browserTest,
      withLinux = false,
      withAndroid = false,
      appleTargets = appleTargets,
  )
}

fun Project.configureMpp(
    withJvm: Boolean,
    withJs: Boolean,
    withLinux: Boolean,
    withAndroid: Boolean,
    appleTargets: Collection<String>,
    browserTest: Boolean = false,
) {
  val kotlinExtension = extensions.findByName("kotlin") as? KotlinMultiplatformExtension
  check(kotlinExtension != null) {
    "No multiplatform extension found"
  }
  kotlinExtension.apply {
    if (withJvm) {
      jvm()
    }

    if (enabledJs && withJs) {
      js(IR) {
        if (browserTest) {
          browser {
            testTask(Action {
              useKarma {
                useChromeHeadless()
              }
            })
          }
        } else {
          nodejs {
            testTask(Action {
              useMocha {
                // Override default 2s timeout
                timeout = "120s"
              }
            })
          }
        }
      }
    }

    if (enabledLinux && withLinux) {
      linuxX64("linux")
    }

    if (withAndroid) {
      androidTarget {
        publishAllLibraryVariants()
      }
    }

    createAndConfigureAppleTargets(appleTargets.toSet().intersect(enabledAppleTargets))

    addTestDependencies()

    tasks.withType(KotlinJsIrLink::class.java).configureEach {
      notCompatibleWithConfigurationCache("https://youtrack.jetbrains.com/issue/KT-60311/")
    }
    tasks.withType(KotlinNativeLink::class.java).configureEach {
      notCompatibleWithConfigurationCache("https://youtrack.jetbrains.com/issue/KT-60311/")
    }
  }
}

private fun KotlinMultiplatformExtension.createAndConfigureAppleTargets(presetNames: Collection<String>) {
  if (presetNames.isEmpty()) {
    return
  }

  if (System.getProperty("idea.sync.active") != null) {
    // Early return. Inside intelliJ, only configure one target
    targetFromPreset(presets.getByName(hostTarget), "apple")
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

private fun KotlinMultiplatformExtension.addTestDependencies() {
  sourceSets.getByName("commonTest") {
    dependencies {
      implementation(kotlin("test"))
    }
  }
}
