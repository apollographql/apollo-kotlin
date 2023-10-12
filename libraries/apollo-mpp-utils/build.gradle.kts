plugins {
  id("org.jetbrains.kotlin.multiplatform")
}

apolloLibrary(
    javaModuleName = "com.apollographql.apollo3.mpp",
    withLinux = false
)

kotlin {
  sourceSets {
    findByName("commonMain")?.apply {
      dependencies {
        api(project(":apollo-annotations"))
      }
    }
  }
}

