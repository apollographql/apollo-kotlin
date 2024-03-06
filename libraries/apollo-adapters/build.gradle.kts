plugins {
  id("org.jetbrains.kotlin.multiplatform")
}

apolloLibrary(
    namespace = "com.apollographql.apollo3.adapter",
    withLinux = false,
    withWasm = false
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
  }
}
