plugins {
  id("apollo.library")
  id("org.jetbrains.kotlin.multiplatform")
}

apolloLibrary {
  javaModuleName.set("com.apollographql.apollo3.cache.normalized")
  mpp {
    withLinux.set(false)
  }
}

kotlin {
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
