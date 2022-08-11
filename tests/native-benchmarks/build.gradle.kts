plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("com.apollographql.apollo3")
}

kotlin {
  configureAppleTargets("macosX64", "macosArm64")

  enableNewMemoryManager()

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(libs.apollo.runtime)
        implementation(libs.apollo.normalizedcache)
      }
    }

    val commonTest by getting {
      dependencies {
        implementation(libs.kotlin.test.common)
        implementation(libs.kotlin.test.annotations.common)

        implementation(libs.apollo.testingsupport)
        implementation(libs.apollo.mockserver)
        implementation(libs.apollo.mpputils)
      }
    }
  }
}

apollo {
  packageName.set("benchmarks")
  generateDataBuilders.set(true)
}

