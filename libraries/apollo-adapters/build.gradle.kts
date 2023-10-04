plugins {
  id("org.jetbrains.kotlin.multiplatform")
}

apolloLibrary(
    javaModuleName = "com.apollographql.apollo3.adapter",
    withLinux = false
)

kotlin {
  sourceSets {
    findByName("commonMain")?.apply {
      dependencies {
        api(project(":apollo-api"))
        api(libs.kotlinx.datetime)
      }
    }
    findByName("jsMain")?.apply {
      dependencies {
        implementation(npm("big.js", "5.2.2"))
      }
    }
  }
}
