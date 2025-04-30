plugins {
  id("org.jetbrains.kotlin.multiplatform")
}

apolloLibrary(
    namespace = "com.apollographql.apollo.mpp",
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

