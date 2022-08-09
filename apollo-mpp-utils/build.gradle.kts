plugins {
  id("apollo.library.multiplatform")
}

configureMppDefaults(withLinux = false)

kotlin {
  sourceSets {
    val commonMain by getting {
      dependencies {
        api(projects.apolloAnnotations)
      }
    }
  }
}

apolloConvention {
  javaModuleName.set("com.apollographql.apollo3.mpp")
}
