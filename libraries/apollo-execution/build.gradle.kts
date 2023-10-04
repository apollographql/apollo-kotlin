plugins {
  antlr
  id("org.jetbrains.kotlin.multiplatform")
}

apolloLibrary(
  javaModuleName = "com.apollographql.apollo3.execution"
)

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

