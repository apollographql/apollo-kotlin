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

    val appleMain = sourceSets.create("appleMain") {
      it.dependsOn(sourceSets.getByName("commonMain"))
    }
    val appleTest = sourceSets.create("appleTest") {
      it.dependsOn(sourceSets.getByName("commonTest"))
    }

    macosX64()
    iosX64()
    iosArm64()

    sourceSets.getByName("macosX64Main").dependsOn(appleMain)
    sourceSets.getByName("macosX64Test").dependsOn(appleTest)
    sourceSets.getByName("iosX64Main").dependsOn(appleMain)
    sourceSets.getByName("iosX64Test").dependsOn(appleTest)
    sourceSets.getByName("iosArm64Main").dependsOn(appleMain)
    sourceSets.getByName("iosArm64Test").dependsOn(appleTest)

    addTestDependencies(withJs)
  }
}

/**
 * Same as [configureMppDefaults] but without iOS targets. Tests only run on the JVM and MacOS
 */
fun Project.configureMppTestsDefaults() {
  val kotlinExtension = extensions.findByName("kotlin") as? org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
  check(kotlinExtension != null) {
    "No multiplatform extension found"
  }
  kotlinExtension.apply {
    /**
     * configure targets
     */
    jvm()
    macosX64("apple")

    addTestDependencies(false)
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
