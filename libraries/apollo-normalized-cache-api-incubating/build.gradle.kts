plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("kotlinx-atomicfu")
}

apolloLibrary(
    namespace = "com.apollographql.apollo3.cache.normalized.api",
    withLinux = false,
)

kotlin {
  sourceSets {
    findByName("commonMain")?.apply {
      dependencies {
        api(project(":apollo-api"))
        api(project(":apollo-mpp-utils"))
        implementation(libs.okio)
        api(libs.uuid)
        implementation(libs.atomicfu.library)
      }
    }

    findByName("commonTest")?.apply {
      dependencies {
        implementation(project(":apollo-testing-support"))
      }
    }
  }
}
