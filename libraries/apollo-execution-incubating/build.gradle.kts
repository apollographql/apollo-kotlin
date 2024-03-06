plugins {
  id("org.jetbrains.kotlin.multiplatform")
}

apolloLibrary(
    namespace = "com.apollographql.apollo3.execution",
    publish = false,
)

kotlin {
  sourceSets {
    getByName("commonMain") {
      dependencies {
        api(project(":apollo-ast"))
        api(project(":apollo-api"))
        implementation(libs.atomicfu.library)
        implementation(libs.kotlinx.coroutines)
      }
    }
  }
}

