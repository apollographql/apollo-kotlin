plugins {
  id("apollo.test.multiplatform")
}

apolloConvention {
  kotlin(withJs = false, withJvm = false, newMemoryManager = false) {
    sourceSets {
      val commonMain by getting {
        dependencies {
          implementation(libs.apollo.runtime)
        }
      }
    }
  }
}

apollo {
  packageName.set("test")
}
