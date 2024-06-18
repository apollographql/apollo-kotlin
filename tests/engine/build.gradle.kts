
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithHostTests
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

plugins {
  id("org.jetbrains.kotlin.multiplatform")
}

apolloTest()

kotlin {
  sourceSets {
    getByName("commonMain").apply {
      dependencies {
        implementation(libs.apollo.runtime)
      }
    }

    val commonTest = getByName("commonTest").apply {
      dependencies {
        implementation(libs.apollo.testingsupport)
        implementation(libs.apollo.mockserver)
        implementation(libs.turbine)
      }
    }

    val ktorTest = create("ktorTest") {
      dependsOn(commonTest)
      dependencies {
        implementation(libs.apollo.engine.ktor)
        implementation(libs.slf4j.nop)
      }
    }
    val defaultTest = create("defaultTest") {
      dependsOn(commonTest)
    }

    targets.all {
      val defaultTestCompilation = compilations.findByName("test")
      if (defaultTestCompilation != null) {
        defaultTestCompilation.defaultSourceSet.dependsOn(defaultTest)

        when (this ) {
          is KotlinJvmTarget -> {
            val ktorTestCompilation = compilations.create("ktorTest").apply {
              defaultSourceSet.dependsOn(ktorTest)
            }
            testRuns.create("ktor") {
              setExecutionSourceFrom(ktorTestCompilation)
            }
          }
          is KotlinJsIrTarget -> {
            val ktorTestCompilation = compilations.create("ktorTest").apply {
              defaultSourceSet.dependsOn(ktorTest)
            }
            testRuns.create("ktor") {
              setExecutionSourceFrom(ktorTestCompilation)
            }
          }
          is KotlinNativeTargetWithHostTests -> {
            val ktorTestCompilation = compilations.create("ktorTest").apply {
              defaultSourceSet.dependsOn(ktorTest)
            }
            binaries.test("integration") {
              compilation = ktorTestCompilation
            }
            testRuns.create("ktor") {
              setExecutionSourceFrom(binaries.getTest("integration", DEBUG))
            }
          }
        }
      }
    }
  }
}
