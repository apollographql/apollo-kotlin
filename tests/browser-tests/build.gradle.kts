plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("apollo.test")
}

apolloTest {
  mpp {
    this.browserTest.set(true)
    this.withJvm.set(false)
    this.withJs.set(true)
    this.appleTargets.set(emptyList())
  }
}

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
