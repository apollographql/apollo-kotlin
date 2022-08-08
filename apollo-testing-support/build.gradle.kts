plugins {
  kotlin("multiplatform")
}

configureMppDefaults(withLinux = false)

kotlin {
  sourceSets {
    val commonMain by getting {
      dependencies {
        api(projects.apolloApi)
        api(projects.apolloRuntime)
        api(projects.apolloMockserver)
        api(libs.kotlinx.coroutines)
        implementation(libs.atomicfu.get().toString()) {
          because("We need locks in TestNetworkTransportHandler (we don't use the gradle plugin rewrite)")
        }
        implementation(libs.kotlinx.coroutines.test)
      }
    }

    val jvmTest by getting {
      dependencies {
        implementation(libs.truth)
      }
    }

    val jsMain by getting {
      dependencies {
        implementation(libs.kotlinx.nodejs)
        implementation(libs.kotlin.test.js)
        api(okioNodeJs())
      }
    }
    val jsTest by getting {
      dependencies {
        implementation(libs.kotlin.test.js)
      }
    }
  }
}
