import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

fun Project.configureMppDefaults(withJs: Boolean, withLinux: Boolean) {
  configureMpp(
      withJvm = true,
      withJs = withJs,
      withLinux = withLinux,
      appleTargets = setOf(
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
      ),
      kotlinJsCompilerType = KotlinJsCompilerType.BOTH,
      newMemoryManager = null
  )
}


/**
 * Same as [configureMppDefaults] but without Linux targets. Apple targets can be configured.
 * Tests only run on the JVM, JS and macOS
 */
fun Project.configureMppTestsDefaults(
    withJs: Boolean,
    withJvm: Boolean,
    newMemoryManager: Boolean,
    appleTargets: Collection<String>,
) {
  configureMpp(
      withJvm = withJvm,
      withJs = withJs,
      withLinux = false,
      appleTargets = appleTargets,
      kotlinJsCompilerType = KotlinJsCompilerType.IR,
      newMemoryManager = newMemoryManager
  )
}

fun Project.configureMpp(
    withJvm: Boolean,
    withJs: Boolean,
    withLinux: Boolean,
    appleTargets: Collection<String>,
    kotlinJsCompilerType: KotlinJsCompilerType,
    newMemoryManager: Boolean?,
) {
  val kotlinExtension = extensions.findByName("kotlin") as? KotlinMultiplatformExtension
  check(kotlinExtension != null) {
    "No multiplatform extension found"
  }
  kotlinExtension.apply {
    if (withJvm) {
      jvm()
    }

    if (withJs) {
      js(kotlinJsCompilerType) {
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

    configureAppleTargets(appleTargets)

    addTestDependencies()

    if (newMemoryManager == false) setStrictMemoryModel()
  }
}

fun Project.okio(): String {
  val okioVersion = when (getKotlinPluginVersion()) {
    "1.6.10" -> "3.0.0"
    else -> "3.2.0"
  }

  return "com.squareup.okio:okio:$okioVersion"
}

fun Project.okioNodeJs(): String {
  val okioVersion = when (getKotlinPluginVersion()) {
    "1.6.10" -> "3.0.0"
    else -> "3.2.0"
  }

  return "com.squareup.okio:okio-nodefilesystem:$okioVersion"
}

private fun KotlinMultiplatformExtension.configureAppleTargets(presetNames: Collection<String>) {
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

private fun KotlinMultiplatformExtension.addTestDependencies() {
  sourceSets.getByName("commonTest") {
    dependencies {
      implementation(kotlin("test"))
    }
  }
}

// See https://github.com/JetBrains/kotlin/blob/master/kotlin-native/NEW_MM.md
private fun KotlinMultiplatformExtension.setStrictMemoryModel() {
  targets.withType(KotlinNativeTarget::class.java) {
    binaries.all {
      binaryOptions["memoryModel"] = "strict"
    }
  }
}
