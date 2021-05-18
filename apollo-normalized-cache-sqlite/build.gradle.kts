plugins {
  id("com.android.library")
  kotlin("multiplatform")
  id("com.squareup.sqldelight")
}

sqldelight {
  database("ApolloDatabase") {
    packageName = "com.apollographql.apollo.cache.normalized.sql"
    schemaOutputDirectory = file("src/main/sqldelight/schemas")
  }
}

kotlin {
  @Suppress("ClassName")
  data class iOSTarget(val name: String, val preset: String, val id: String)

  val iosTargets = listOf(
      iOSTarget("iosArm", "iosArm64", "ios-arm64"),
      iOSTarget("iosSim", "iosX64", "ios-x64")
  )

  val appleMain = sourceSets.create("iosMain")
  val appleTest = sourceSets.create("iosTest")
  for ((targetName, presetName, id) in iosTargets) {
    targetFromPreset(presets.getByName(presetName), targetName) {
      mavenPublication {
        artifactId = "${project.name}-$id"
      }
      compilations.getByName("main").source(appleMain)
      compilations.getByName("test").source(appleTest)
    }
  }

  android {
    publishAllLibraryVariants()
  }
  jvm()

  sourceSets {
    val commonMain by getting {
      dependencies {
        api(project(":apollo-api"))
        api(project(":apollo-normalized-cache-api"))
      }
    }

    val jvmMain by getting {
      dependsOn(commonMain)
      dependencies {
        implementation(groovy.util.Eval.x(project, "x.dep.sqldelight.jvm"))
      }
    }

    val androidMain by getting {
      dependsOn(commonMain)
      dependencies {
        api(groovy.util.Eval.x(project, "x.dep.androidx.sqlite"))
        implementation(groovy.util.Eval.x(project, "x.dep.sqldelight.android"))
        implementation(groovy.util.Eval.x(project, "x.dep.androidx.sqliteFramework"))
      }
    }

    val iosMain by getting {
      dependencies {
        implementation(groovy.util.Eval.x(project, "x.dep.sqldelight.native"))
      }
    }

    val commonTest by getting {
      dependencies {
        implementation(kotlin("test-common"))
        implementation(kotlin("test-annotations-common"))
      }
    }

    val jvmTest by getting {
      dependsOn(commonTest)
      dependencies {
        implementation(kotlin("test-junit"))

        implementation(groovy.util.Eval.x(project, "x.dep.junit"))
        implementation(groovy.util.Eval.x(project, "x.dep.truth"))
      }
    }

    val androidTest by getting {
      dependsOn(jvmTest)
    }
  }
}

android {
  compileSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.compileSdkVersion").toString().toInt())

  lintOptions {
    textReport = true
    textOutput("stdout")
    ignore("InvalidPackage")
  }

  defaultConfig {
    minSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.minSdkVersion").toString())
    targetSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.targetSdkVersion").toString())
  }
}


tasks.withType<Javadoc> {
  options.encoding = "UTF-8"
}

