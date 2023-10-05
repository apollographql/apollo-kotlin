plugins {
  id("org.jetbrains.kotlin.multiplatform")
}

apolloLibrary(
    javaModuleName = "com.apollographql.apollo3.engine.ktor",
    withLinux = false
)

kotlin {
  sourceSets {
    findByName("commonMain")?.apply {
      dependencies {
        api(project(":apollo-runtime"))
        api(libs.ktor.client.core)
        api(libs.ktor.client.websockets)
      }
    }

    findByName("jvmMain")?.apply {
      dependencies {
        api(libs.ktor.client.okhttp)
      }
    }

    findByName("appleMain")?.apply {
      dependencies {
        api(libs.ktor.client.darwin)
      }
    }
  }
}
