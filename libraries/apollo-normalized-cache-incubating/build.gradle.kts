plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("apollo.library")
}

apolloLibrary {
  javaModuleName("com.apollographql.apollo3.cache.normalized")
  mpp {
    withLinux.set(false)
  }
}

kotlin {
  sourceSets {
    val commonMain by getting {
      dependencies {
        api(project(":libraries:apollo-runtime"))
        api(project(":libraries:apollo-normalized-cache-api-incubating"))
        api(golatac.lib("kotlinx.coroutines"))
        implementation(golatac.lib("atomicfu")) {
          because("Use of ReentrantLock in DefaultApolloStore for Apple (we don't use the gradle plugin rewrite)")
        }
      }
    }
  }
}
