plugins {
  id("org.jetbrains.kotlin.multiplatform")
}

apolloLibrary(
    javaModuleName = "com.apollographql.apollo3.testing",
    withLinux = false,
)

kotlin {
  sourceSets {
    findByName("commonMain")?.apply {
      dependencies {
        api(project(":apollo-api"))
        api(project(":apollo-runtime"))
        api(project(":apollo-mockserver"))
        api(libs.kotlinx.coroutines)
        implementation(libs.atomicfu.get().toString()) {
          because("We need locks in TestNetworkTransportHandler (we don't use the gradle plugin rewrite)")
        }
        implementation(libs.kotlinx.coroutines.test)
      }
    }

    findByName("jvmTest")?.apply {
      dependencies {
        implementation(libs.truth)
      }
    }

    findByName("jsMain")?.apply {
      dependencies {
        implementation(libs.kotlin.test.js)
        api(libs.okio.nodefilesystem)
      }
    }
    findByName("jsTest")?.apply {
      dependencies {
        implementation(libs.kotlin.test.js)
      }
    }
  }
}
