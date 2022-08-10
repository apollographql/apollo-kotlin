plugins {
  id("apollo.library.multiplatform")
}

apolloConvention {
  javaModuleName.set("com.apollographql.apollo3.cache.normalized.api")

  kotlin(withLinux = false) {
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
}
