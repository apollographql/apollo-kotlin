import org.gradle.api.Project

fun Project.configureMppDefaults(withJs: Boolean = true) {
  val kotlinExtension = extensions.findByName("kotlin") as? org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
  check(kotlinExtension != null) {
    "No multiplatform extension found"
  }
  kotlinExtension.apply {
    /**
     * configure targets
     */
    jvm()

    if (withJs) {
      js {
        useCommonJs()
        browser()
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
      iosX64().apply {
        compilations.getByName("main").source(appleMain)
        compilations.getByName("test").source(appleTest)
      }
      iosArm64().apply {
        compilations.getByName("main").source(appleMain)
        compilations.getByName("test").source(appleTest)
      }
    } else {
      // We are in intelliJ
      // Make intelliJ believe we have a single target with all the code in "apple" sourceSets
      macosX64("apple")
    }

    /**
     * configure tests
     */
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
}