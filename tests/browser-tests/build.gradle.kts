plugins {
  id("org.jetbrains.kotlin.multiplatform")
}

apolloTest(
    browserTest = true,
    withJvm = true,
    withJs = true,
    appleTargets = emptySet()
)

kotlin {
  sourceSets {
    val commonTest by getting {
      dependencies {
        implementation(libs.apollo.runtime)
        implementation(libs.kotlinx.coroutines.test)
      }
    }
  }
}
