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
    findByName("commonMain")?.apply {
      dependencies {
        api(okio())
        api(golatac.lib("uuid"))
        api(project(":libraries:apollo-annotations"))
      }
    }
  }
}

