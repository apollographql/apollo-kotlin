plugins {
  id("org.jetbrains.kotlin.multiplatform")
}

apolloLibrary(
    javaModuleName = "com.apollographql.apollo3.runtime",
    withLinux = false
)

kotlin {
  sourceSets {
    findByName("commonMain")?.apply {
      dependencies {
        api(project(":apollo-api"))
        api(project(":apollo-mpp-utils"))
        implementation(libs.atomicfu.library) // https://github.com/Kotlin/kotlinx.coroutines/issues/3968
        api(libs.okio)
        api(libs.uuid)
        api(libs.kotlinx.coroutines)
      }
    }

    findByName("commonTest")?.apply {
      dependencies {
        implementation(project(":apollo-mockserver"))
        implementation(project(":apollo-testing-support")) {
          because("runTest")
          // We have a circular dependency here that creates a warning in JS
          // w: duplicate library name: com.apollographql.apollo3:apollo-mockserver
          // See https://youtrack.jetbrains.com/issue/KT-51110
          // We should probably remove this circular dependency but for the time being, just use excludes
          exclude(group = "com.apollographql.apollo3", module = "apollo-runtime")
        }
      }
    }

    findByName("jvmMain")?.apply {
      dependencies {
        api(libs.okhttp)
      }
    }

    findByName("jsMain")?.apply {
      dependencies {
        api(libs.ktor.client.js)
      }
    }

    findByName("appleMain")?.apply {
      dependencies {
      }
    }

    findByName("jvmTest")?.apply {
      dependencies {
        implementation(libs.kotlin.test.junit)
        implementation(libs.truth)
        implementation(libs.okhttp)
      }
    }
  }
}

tasks.register("iOSSimTest") {
  dependsOn("iosSimTestBinaries")
  doLast {
    val binary = kotlin.targets.getByName<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>("iosSim").binaries.getTest("DEBUG").outputFile
    exec {
      commandLine = listOf("xcrun", "simctl", "spawn", "iPhone 8", binary.absolutePath)
    }
  }
}
