plugins {
  id("org.jetbrains.kotlin.multiplatform")
}

apolloLibrary(
  javaModuleName = "com.apollographql.apollo3.cache.normalized.api",
    withLinux = false,
    publish = false,
)

kotlin {
  sourceSets {
    findByName("commonMain")?.apply {
      dependencies {
        api(project(":apollo-api"))
        api(project(":apollo-mpp-utils"))
        implementation(libs.okio)
        api(libs.uuid)
      }
    }
  }
}
