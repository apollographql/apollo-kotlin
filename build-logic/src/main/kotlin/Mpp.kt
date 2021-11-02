import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

fun Project.configureMppDefaults(withJs: Boolean = true, withLinux: Boolean = true) {
  // See https://kotlinlang.org/docs/mpp-dsl-reference.html#targets

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

    if (System.getProperty("idea.sync.active") == null) {
      val appleMain = sourceSets.create("appleMain")
      val appleTest = sourceSets.create("appleTest")

      macosX64().apply {
        compilations.getByName("main").source(appleMain)
        compilations.getByName("test").source(appleTest)
      }
      macosArm64().apply {
        compilations.getByName("main").source(appleMain)
        compilations.getByName("test").source(appleTest)
      }

      iosArm64().apply {
        compilations.getByName("main").source(appleMain)
        compilations.getByName("test").source(appleTest)
      }
      iosX64().apply {
        compilations.getByName("main").source(appleMain)
        compilations.getByName("test").source(appleTest)
      }
      iosSimulatorArm64().apply {
        compilations.getByName("main").source(appleMain)
        compilations.getByName("test").source(appleTest)
      }

      watchosArm64().apply {
        compilations.getByName("main").source(appleMain)
        compilations.getByName("test").source(appleTest)
      }
      watchosSimulatorArm64().apply {
        compilations.getByName("main").source(appleMain)
        compilations.getByName("test").source(appleTest)
      }

      tvosArm64().apply {
        compilations.getByName("main").source(appleMain)
        compilations.getByName("test").source(appleTest)
      }
      tvosX64().apply {
        compilations.getByName("main").source(appleMain)
        compilations.getByName("test").source(appleTest)
      }
      tvosSimulatorArm64().apply {
        compilations.getByName("main").source(appleMain)
        compilations.getByName("test").source(appleTest)
      }
    } else {
      // We are in intelliJ
      // Make intelliJ believe we have a single target with all the code in "apple" sourceSets
      macosX64("apple")
    }

    if (withLinux) {
      linuxX64("linux")
    }

    addTestDependencies(withJs)

    if (System.getProperty("idea.sync.active") == null) {
      /**
       * Evil tasks to fool IntelliJ into running the appropriate tests when clicking the green triangle in the gutter
       * IntelliJ "sees" apple during sync but the actual tasks are macosX64
       */
      tasks.register("cleanAppleTest") {
        it.dependsOn("cleanMacosX64Test")
      }
      tasks.register("appleTest") {
        it.dependsOn("macosX64Test")
      }
    }
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
        nodejs()
      }
    }
    macosX64("apple")

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
