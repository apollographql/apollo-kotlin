plugins {
  id("com.apollographql.apollo3")
  kotlin("multiplatform")
}

kotlin {
  configureAppleTargets("macosX64", "macosArm64")

  sourceSets {
    val commonMain by getting {
      dependencies {
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

apollo {
  packageName.set("test")
}

