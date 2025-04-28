plugins {
  id("org.jetbrains.kotlin.multiplatform")
}

apolloLibrary(
    namespace = "com.apollographql.apollo.testing.internal",
    publish = false
)

kotlin {
  sourceSets {
    findByName("commonMain")?.apply {
      dependencies {
        api(project(":apollo-api"))
        api(libs.kotlinx.coroutines)
        api(libs.kotlinx.coroutines.test)
      }
    }
    findByName("jsMain")?.apply {
      dependencies {
        implementation(libs.kotlin.test.js)
        api(libs.okio.nodefilesystem)
      }
    }
    findByName("jsTest")?.apply {
      dependencies {
        implementation(libs.kotlin.test.js)
      }
    }
  }
}
