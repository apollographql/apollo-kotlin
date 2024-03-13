plugins {
  id("org.jetbrains.kotlin.multiplatform")
}

apolloLibrary(
    namespace = "com.apollographql.apollo3.api"
)

kotlin {
  sourceSets {
    findByName("commonMain")?.apply {
      dependencies {
        api(libs.okio)
        api(libs.uuid)
        api(project(":apollo-annotations"))
      }
    }
  }
}

