plugins {
  id("apollo.test.multiplatform")
}

apolloConvention {
  kotlin(withJs = false, withJvm = false) {
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

