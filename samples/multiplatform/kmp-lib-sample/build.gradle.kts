import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("multiplatform")
  id("com.apollographql.apollo3")
}

group = "com.apollographql.apollo3.kmpsample"
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
        implementation("com.apollographql.apollo3:apollo-api")
        implementation("com.apollographql.apollo3:apollo-runtime-kotlin")
        implementation(groovy.util.Eval.x(project, "x.dep.kotlin.coroutines"))
      }
    }
    val jvmMain by getting {
      dependencies {
      }
    }
    val iosMain by getting {
      dependencies {
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
  kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
}

tasks.register("copyFramework", Sync::class) {
  val targetDir = File(buildDir, "xcode-frameworks")
  val mode = System.getenv("CONFIGURATION") ?: "DEBUG"
  val framework = kotlin.targets
      .getByName<KotlinNativeTarget>("ios")
      .binaries.getFramework(mode)

  inputs.property("mode", mode)
  dependsOn(framework.linkTask)

  from({ framework.outputDirectory })
  into(targetDir)

  doLast {
    val gradlew = File(targetDir, "gradlew")
    gradlew.writeText("#!/bin/bash\n"
        + "export 'JAVA_HOME=${System.getProperty("java.home")}'\n"
        + "cd '${rootProject.rootDir}'\n"
        + "./gradlew \$@ --no-configure-on-demand\n")
    gradlew.setExecutable(true)
  }
}

apollo {
  service("kmpsample")
  generateFragmentImplementations.set(true)
}