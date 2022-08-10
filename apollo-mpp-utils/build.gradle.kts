plugins {
  id("apollo.library.multiplatform")
}

apolloConvention {
  javaModuleName.set("com.apollographql.apollo3.mpp")

  kotlin(withLinux = false) {
    sourceSets {
      val commonMain by getting {
        dependencies {
          api(projects.apolloAnnotations)
        }
      }
    }
  }
}
