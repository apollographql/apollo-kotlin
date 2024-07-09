plugins {
  id("org.jetbrains.kotlin.multiplatform")
}

apolloLibrary(
    namespace = "com.apollographql.apollo.engine.tests",
    withLinux = false,
)

kotlin {
  sourceSets {
    findByName("commonMain")?.apply {
      dependencies {
        api(project(":apollo-runtime"))
        implementation(libs.kotlin.test)
        implementation(libs.apollo.mockserver)
      }
    }
    findByName("commonTest")?.apply {
      dependencies {
        implementation(project(":apollo-testing-support"))
      }
    }
    findByName("jvmTest")?.apply {
      dependencies {
        implementation(libs.kotlin.test.junit)
      }
    }
  }
}
