import com.gradleup.librarian.core.tooling.init.kotlinPluginVersion

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
        implementation(libs.apollo.mockserver)
        implementation("org.jetbrains.kotlin:kotlin-test:$kotlinPluginVersion")
      }
    }
    findByName("commonTest")?.apply {
      dependencies {
        implementation(project(":apollo-testing-support-internal"))
      }
    }
    findByName("jvmTest")?.apply {
      dependencies {
        implementation(libs.kotlin.test.junit)
      }
    }
  }
}
