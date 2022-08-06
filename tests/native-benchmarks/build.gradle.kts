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
        implementation(libs.apollo.runtime)
        implementation(libs.apollo.normalizedCache)
      }
    }

    val commonTest by getting {
      dependencies {
        implementation(libs.kotlin.test.common)
        implementation(libs.kotlin.test.annotations.common)

        implementation(libs.apollo.testingSupport)
        implementation(libs.apollo.mockserver)
        implementation(libs.apollo.mppUtils)
      }
    }
  }
}

apollo {
  packageName.set("benchmarks")
}

