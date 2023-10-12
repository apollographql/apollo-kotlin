plugins {
  id("org.jetbrains.kotlin.multiplatform")
}

apolloLibrary(
  javaModuleName = "com.apollographql.apollo3.cache.normalized",
    withLinux = false
)

kotlin {
  sourceSets {
    findByName("commonMain")?.apply {
      dependencies {
        api(project(":apollo-runtime"))
        api(project(":apollo-normalized-cache-api"))
        api(libs.kotlinx.coroutines)
        implementation(libs.atomicfu.get().toString()) {
          because("Use of ReentrantLock in DefaultApolloStore for Apple (we don't use the gradle plugin rewrite)")
        }
      }
    }
  }
}
