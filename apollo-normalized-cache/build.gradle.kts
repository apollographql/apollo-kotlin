plugins {
  id("apollo.library.multiplatform")
}

apolloConvention {
  javaModuleName.set("com.apollographql.apollo3.cache.normalized")

  kotlin(withLinux = false) {
    sourceSets {
      val commonMain by getting {
        dependencies {
          api(projects.apolloRuntime)
          api(projects.apolloNormalizedCacheApi)
          api(libs.kotlinx.coroutines)
          implementation(libs.atomicfu.get().toString()) {
            because("Use of ReentrantLock in DefaultApolloStore for Apple (we don't use the gradle plugin rewrite)")
          }
        }
      }
    }
  }
}
