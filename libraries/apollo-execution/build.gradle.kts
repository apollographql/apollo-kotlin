plugins {
  antlr
  id("org.jetbrains.kotlin.multiplatform")
  id("apollo.library")
}

apolloLibrary {
  javaModuleName("com.apollographql.apollo3.execution")
  mpp {}
}

kotlin {
  sourceSets {
    getByName("commonMain") {
      dependencies {
        api(project(":apollo-ast"))
        api(project(":apollo-api"))
        implementation(libs.atomicfu)
      }
    }
  }
}

