plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("com.apollographql.apollo3")
}

kotlin {
  configureAppleTargets("iosArm64", "iosX64")
  enableNewMemoryManager()
  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(libs.apollo.runtime)
        implementation(libs.apollo.mockserver)
      }
    }

    val commonTest by getting {
      dependencies {
        implementation(libs.kotlin.test.junit)
        implementation(libs.apollo.testingsupport)
      }
    }
  }
}

apollo {
  packageName.set("ios.test")
}
