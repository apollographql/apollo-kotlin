plugins {
  id(libs.plugins.apollo.get().toString())
  id(libs.plugins.kotlin.multiplatform.get().toString())
}

kotlin {
  configureAppleTargets("macosX64", "macosArm64")

  enableNewMemoryManager()

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(libs.apollo.testingSupport)
        implementation(libs.apollo.mockserver)
        implementation(libs.apollo.normalizedCache)
        implementation(libs.apollo.mppUtils)
        implementation(libs.apollo.runtime)
      }
    }

    val commonTest by getting {
      dependencies {
        implementation(libs.kotlin.test.common)
        implementation(libs.kotlin.test.annotations.common)
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
}

