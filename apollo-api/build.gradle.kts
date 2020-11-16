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

  js {
    useCommonJs()
    browser()
    nodejs()
  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        api(groovy.util.Eval.x(project, "x.dep.okio"))
      }
    }

    val jvmMain by getting {
      dependsOn(commonMain)
      dependencies {
      }
    }

    val iosMain by getting {
      dependsOn(commonMain)
    }

    val iosSimMain by getting {
      dependsOn(iosMain)
    }

    val jsMain by getting {
      dependsOn(commonMain)
      dependencies {
        implementation(npm("big.js", "5.2.2"))
      }
    }

    val jsTest by getting {
      dependencies {
        implementation(kotlin("test-js"))
      }
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

metalava {
  hiddenPackages += setOf("com.apollographql.apollo.api.internal")
}

tasks.withType<Checkstyle> {
  exclude("**/BufferedSourceJsonReader.java")
  exclude("**/JsonScope.java")
  exclude("**/JsonUtf8Writer.java")
}

tasks.named("javadoc").configure {
  /**
   * Somehow Javadoc fails when I removed the `@JvmSynthetic` annotation from `InputFieldWriter.ListItemWriter.writeList`
   * It fails with `javadoc: error - String index out of range: -1`
   * Javadoc from JDK 13 works fine
   * I'm not sure how to fix it so this ignores the error. The uploaded javadoc.jar will be truncated and only contain the
   * classes that have been written successfully before Javadoc fails.
   */
  (this as Javadoc).isFailOnError = false
}
