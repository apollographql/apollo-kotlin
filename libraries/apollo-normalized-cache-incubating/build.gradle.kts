plugins {
  id("org.jetbrains.kotlin.multiplatform")
}

apolloLibrary(
    javaModuleName = "com.apollographql.apollo3.cache.normalized",
    withLinux = false,
)

kotlin {
  sourceSets {
    findByName("commonMain")?.apply {
      dependencies {
        api(project(":apollo-runtime"))
        api(project(":apollo-normalized-cache-api-incubating"))
        api(libs.kotlinx.coroutines)
      }
    }
  }
}
