plugins {
  id(libs.plugins.apollo.get().toString())
  id(libs.plugins.kotlin.multiplatform.get().toString())
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
        implementation(libs.apollo.testingSupport)
      }
    }
  }
}

apollo {
  packageName.set("ios.test")
}
