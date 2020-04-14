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
        api(project(":apollo-api"))
        implementation(kotlin("stdlib-common"))
        implementation(groovy.util.Eval.x(project, "x.dep.okio.okioMultiplatform"))
        api(groovy.util.Eval.x(project, "x.dep.uuid"))
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

    val jvmTest by getting {
      dependsOn(jvmMain)
      dependencies {
        implementation(groovy.util.Eval.x(project, "x.dep.junit"))
        implementation(groovy.util.Eval.x(project, "x.dep.truth"))
      }
    }
  }
}

tasks.withType<Javadoc> {
  options.encoding = "UTF-8"
}

