plugins {
  id("apollo.library.multiplatform")
}

apolloConvention {
  javaModuleName.set("com.apollographql.apollo3.api")

  kotlin {
    sourceSets {
      val commonMain by getting {
        dependencies {
          api(okio())
          api(libs.uuid)
          api(projects.apolloAnnotations)
        }
      }
    }
  }
}
