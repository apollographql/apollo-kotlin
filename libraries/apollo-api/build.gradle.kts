plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("apollo.library")
}

apolloLibrary {
  javaModuleName("com.apollographql.apollo3.api")
  mpp {}
}

kotlin {
  sourceSets {
    val commonMain by getting {
      dependencies {
        api(okio())
        api(golatac.lib("uuid"))
        api(project(":libraries:apollo-annotations"))
      }
    }
  }
}

