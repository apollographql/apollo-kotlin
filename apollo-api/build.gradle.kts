import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  `java-library`
  kotlin("multiplatform")
}

kotlin {
  @Suppress("ClassName")
  data class iOSTarget(val name: String, val preset: String, val id: String)

  val iosTargets = listOf(
      iOSTarget("ios", "iosArm64", "ios-arm64"),
      iOSTarget("iosSim", "iosX64", "ios-x64")
  )

  for ((targetName, presetName, id) in iosTargets) {
    targetFromPreset(presets.getByName(presetName), targetName) {
      mavenPublication {
        artifactId = "${project.name}-$id"
      }
    }
  }

  jvm {
    withJava()
  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(kotlin("stdlib-common"))
        api(groovy.util.Eval.x(project, "x.dep.okio.okioMultiplatform"))
      }
    }

    val jvmMain by getting {
      dependsOn(commonMain)
      dependencies {
        implementation(kotlin("stdlib"))
      }
    }

    val iosMain by getting {
      dependsOn(commonMain)
    }

    val iosSimMain by getting {
      dependsOn(iosMain)
    }

    val commonTest by getting {
      dependencies {
        implementation(kotlin("test-common"))
        implementation(kotlin("test-annotations-common"))
      }
    }

    val jvmTest by getting {
      dependsOn(jvmMain)
      dependencies {
        implementation(kotlin("test-junit"))
        implementation(groovy.util.Eval.x(project, "x.dep.truth"))
        implementation(groovy.util.Eval.x(project, "x.dep.okHttp.okHttp"))
      }
    }
  }
}

tasks.withType<Checkstyle> {
  exclude("**/BufferedSourceJsonReader.java")
  exclude("**/JsonScope.java")
  exclude("**/JsonUtf8Writer.java")
}
