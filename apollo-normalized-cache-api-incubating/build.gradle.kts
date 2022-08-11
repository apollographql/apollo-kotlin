plugins {
  id("apollo.library")
  id("org.jetbrains.kotlin.multiplatform")
}

apolloLibrary {
  javaModuleName.set("com.apollographql.apollo3.cache.normalized.api")
  mpp {
    withLinux.set(false)
  }
}

kotlin {
  sourceSets {
    val commonMain by getting {
      dependencies {
        api(projects.apolloApi)
        api(projects.apolloMppUtils)
        implementation(okio())
        api(libs.uuid)
      }
    }
  }
}
