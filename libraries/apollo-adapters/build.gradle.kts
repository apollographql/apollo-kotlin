plugins {
  id("org.jetbrains.kotlin.multiplatform")
}

apolloLibrary(
    namespace = "com.apollographql.apollo.adapter",
    withLinux = false,
)

kotlin {
  sourceSets {
    findByName("commonMain")?.apply {
      dependencies {
        api(project(":apollo-api"))
        api(libs.kotlinx.datetime)
        api(project(":apollo-annotations"))
      }
    }
    findByName("jsMain")?.apply {
      dependencies {
        implementation(npm("big.js", "5.2.2"))
      }
    }
    findByName("wasmJsMain")?.apply {
      dependencies {
        implementation(npm("big.js", "5.2.2"))
      }
    }
  }
}
