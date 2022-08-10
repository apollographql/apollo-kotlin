plugins {
  id("apollo.library.multiplatform")
}

apolloConvention {
  javaModuleName.set("com.apollographql.apollo3.runtime")

  kotlin(withLinux = false) {
    sourceSets {
      val commonMain by getting {
        dependencies {
          api(projects.apolloApi)
          api(projects.apolloMppUtils)
          api(okio())
          api(libs.uuid)
          api(libs.kotlinx.coroutines)
        }
      }

      val commonTest by getting {
        dependencies {
          implementation(projects.apolloMockserver)
          implementation(projects.apolloTestingSupport)
        }
      }

      val jvmMain by getting {
        dependencies {
          api(libs.okhttp)
        }
      }

      val jsMain by getting {
        dependencies {
          api(libs.ktor.client.js)
        }
      }

      val appleMain by getting {
        dependencies {
        }
      }

      val jvmTest by getting {
        dependencies {
          implementation(libs.kotlin.test.junit)
          implementation(libs.truth)
          implementation(libs.okhttp)
        }
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
