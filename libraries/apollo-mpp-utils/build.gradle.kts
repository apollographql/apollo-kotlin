plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("apollo.library")
}

apolloLibrary {
  javaModuleName("com.apollographql.apollo3.mpp")
  mpp {
    withLinux.set(false)
  }
}

kotlin {
  sourceSets {
    val commonMain by getting {
      dependencies {
        api(project(":libraries:apollo-annotations"))
      }
    }
  }
}

