plugins {
  id("apollo.test.multiplatform")
}

apolloConvention {
  kotlin(withJs = false, withJvm = false, appleTargets = setOf("iosArm64", "iosX64")) {
    sourceSets {
      val commonMain by getting {
        dependencies {
          implementation(libs.apollo.runtime)
          implementation(libs.apollo.mockserver)
        }
      }

      val commonTest by getting {
        dependencies {
          implementation(libs.apollo.testingsupport)
        }
      }
    }
  }
}

apollo {
  packageName.set("ios.test")
}
