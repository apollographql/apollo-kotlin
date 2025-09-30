import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion

plugins {
  id("org.jetbrains.kotlin.multiplatform")
}

apolloLibrary(
    namespace = "com.apollographql.apollo.engine.tests",
    withLinux = false,
    withJs = true,
    enableWasmJsTests = false
)

kotlin {
  sourceSets {
    findByName("commonMain")?.apply {
      dependencies {
        api(project(":apollo-runtime"))
        implementation(libs.apollo.mockserver)
        implementation("org.jetbrains.kotlin:kotlin-test:${getKotlinPluginVersion()}")
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
