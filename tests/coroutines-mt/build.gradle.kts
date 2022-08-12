plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("apollo.test")
  id("com.apollographql.apollo3")
}

apolloTest {
  mpp {
    withJs.set(false)
    withJvm.set(false)
  }
}

kotlin {
  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(libs.apollo.testingsupport)
        implementation(libs.apollo.mockserver)
        implementation(libs.apollo.normalizedcache)
        implementation(libs.apollo.mpputils)
        implementation(libs.apollo.runtime)
      }
    }
  }
}

configurations {
  all {
    resolutionStrategy {
      force("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.3-native-mt")
      force("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.3")
    }
  }
}

apollo {
  packageName.set("macos.app")
  generateDataBuilders.set(true)
}

