plugins {
  id("apollo.library.multiplatform")
}

configureMppDefaults(withLinux = false)

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

apolloConvention {
  javaModuleName.set("com.apollographql.apollo3.cache.normalized.api")
}
