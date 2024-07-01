plugins {
  id("org.jetbrains.kotlin.multiplatform")
}

apolloLibrary(
  namespace = "com.apollographql.apollo.cache.normalized",
    withLinux = false
)

kotlin {
  sourceSets {
    findByName("commonMain")?.apply {
      dependencies {
        api(project(":apollo-runtime"))
        api(project(":apollo-normalized-cache-api"))
        api(libs.kotlinx.coroutines)
        implementation(libs.atomicfu.library.get().toString()) {
          because("Use of ReentrantLock in DefaultApolloStore for Apple (we don't use the gradle plugin rewrite)")
        }
      }
    }
  }
}
