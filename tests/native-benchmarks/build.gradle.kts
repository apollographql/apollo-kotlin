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
        implementation("com.apollographql.apollo3:apollo-runtime")
        implementation("com.apollographql.apollo3:apollo-normalized-cache")
      }
    }

    val commonTest by getting {
      dependencies {
        implementation(libs.kotlin.test.common)
        implementation(libs.kotlin.test.annotations.common)

        implementation("com.apollographql.apollo3:apollo-testing-support")
        implementation("com.apollographql.apollo3:apollo-mockserver")
        implementation("com.apollographql.apollo3:apollo-mpp-utils")
      }
    }
  }
}

apollo {
  packageName.set("benchmarks")
}

