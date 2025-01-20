plugins {
  id("org.jetbrains.kotlin.multiplatform")
}

apolloLibrary(
    namespace = "com.apollographql.apollo.execution",
)

kotlin {
  sourceSets {
    getByName("commonMain") {
      dependencies {
        api(libs.kotlinx.coroutines)
        implementation(libs.atomicfu.library) // for arrow and the LRU cache
        api(project(":apollo-api"))
        api(project(":apollo-ast"))
      }
    }
  }
}
