plugins {
  id("com.apollographql.apollo3")
  kotlin("multiplatform")
}

kotlin {
  configureAppleTargets("macosX64", "macosArm64")

  enableNewMemoryManager()

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation("com.apollographql.apollo3:apollo-testing-support")
        implementation("com.apollographql.apollo3:apollo-mockserver")
        implementation("com.apollographql.apollo3:apollo-normalized-cache")
        implementation("com.apollographql.apollo3:apollo-mpp-utils")
        implementation("com.apollographql.apollo3:apollo-runtime")
      }
    }

    val commonTest by getting {
      dependencies {
        implementation(kotlin("test-common"))
        implementation(kotlin("test-annotations-common"))
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

