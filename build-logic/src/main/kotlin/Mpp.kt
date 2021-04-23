import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

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