plugins {
  id("apollo.library.multiplatform")
}

configureMppDefaults()

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

apolloConvention {
  javaModuleName.set("com.apollographql.apollo3.api")
}
