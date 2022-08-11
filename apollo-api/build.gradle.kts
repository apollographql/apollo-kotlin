plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("apollo.library")
}

apolloLibrary {
  javaModuleName.set("com.apollographql.apollo3.api")
  mpp {}
}

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

