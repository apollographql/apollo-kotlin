plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("apollo.library")
}

apolloLibrary {
  javaModuleName("com.apollographql.apollo3.cache.normalized.api")
  mpp {
    withLinux.set(false)
  }
}

kotlin {
  sourceSets {
    val commonMain by getting {
      dependencies {
        api(project(":libraries:apollo-api"))
        api(project(":libraries:apollo-mpp-utils"))
        implementation(okio())
        api(golatac.lib("uuid"))
      }
    }
  }
}
