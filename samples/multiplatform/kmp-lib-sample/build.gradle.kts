import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("multiplatform")
  id("com.apollographql.apollo")
}

group = "com.apollographql.apollo.kmpsample"
version = 1.0

kotlin {
  jvm()

  val buildForDevice = project.findProperty("device") as? Boolean ?: false
  val iosTarget = if (buildForDevice) iosArm64("ios") else iosX64("ios")
  iosTarget.binaries {
    framework {
      // Disable bitcode embedding for the simulator build.
      if (!buildForDevice) {
        embedBitcode("disable")
      }
    }
  }

  sourceSets {
    commonMain {
      dependencies {
        implementation(kotlin("stdlib-common"))
        implementation("com.apollographql.apollo:apollo-api")
      }
    }
    commonTest {
      dependencies {
        implementation(kotlin("test-common"))
        implementation(kotlin("test-annotations-common"))
      }
    }
  }
}

tasks.withType<KotlinCompile> {
  kotlinOptions.jvmTarget = "1.8"
}

tasks.register("copyFramework") {
  val buildType = project.findProperty("kotlin.build.type") as? String ?: "DEBUG"
  dependsOn("link${buildType.toLowerCase().capitalize()}FrameworkIos")

  doLast {
    val srcFile = (kotlin.targets["ios"] as KotlinNativeTarget).binaries.getFramework(buildType).outputFile
    val targetDir = project.property("configuration.build.dir")!!
    copy {
      from(srcFile.parent)
      into(targetDir)
      include("sample.framework/**")
      include("sample.framework.dSYM")
    }
  }
}
