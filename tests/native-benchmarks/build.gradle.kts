plugins {
  id("apollo.test.multiplatform")
}
apolloConvention {
  kotlin(withJs = false, withJvm = false) {
    sourceSets {
      val commonMain by getting {
        dependencies {
          implementation(libs.apollo.runtime)
          implementation(libs.apollo.normalizedcache)
        }
      }

      val commonTest by getting {
        dependencies {
          implementation(libs.apollo.testingsupport)
          implementation(libs.apollo.mockserver)
          implementation(libs.apollo.mpputils)
        }
      }
    }
  }
}

apollo {
  packageName.set("benchmarks")
}
