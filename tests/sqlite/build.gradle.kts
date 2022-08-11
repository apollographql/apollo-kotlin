plugins {
  id("apollo.test.multiplatform")
}

apolloConvention {
  kotlin(withJs = false) {
    sourceSets {
      val commonMain by getting {
        dependencies {
          implementation(libs.apollo.runtime)
        }
      }

      val commonTest by getting {
        dependencies {
          implementation(libs.apollo.testingsupport)
          implementation(libs.apollo.normalizedcache.incubating)
          implementation(libs.apollo.normalizedcache.sqlite.incubating)
        }
      }
    }
  }
}

apollo {
  packageName.set("sqlite")
}
