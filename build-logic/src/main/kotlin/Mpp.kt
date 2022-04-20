import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

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
        nodejs()
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

fun KotlinMultiplatformExtension.configureAppleTargets(vararg presetNames: String) {
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
