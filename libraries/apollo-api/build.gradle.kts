plugins {
  id("org.jetbrains.kotlin.multiplatform")
}

apolloLibrary(
    javaModuleName = "com.apollographql.apollo3.api"
)

kotlin {
  sourceSets {
    findByName("commonMain")?.apply {
      dependencies {
        api(libs.okio)
        api(libs.uuid)
        api(project(":apollo-annotations"))
      }
    }

    findByName("commonTest")?.apply {
      dependencies {
        implementation(libs.kotlin.test.asProvider().get().toString())
      }
    }
  }
}

