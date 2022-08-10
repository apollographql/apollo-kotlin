plugins {
  id("apollo.library.multiplatform")
}

apolloConvention {
  javaModuleName.set("com.apollographql.apollo3.adapter")

  kotlin(withLinux = false) {
    sourceSets {
      val commonMain by getting {
        dependencies {
          api(projects.apolloApi)
          api(libs.kotlinx.datetime)
        }
      }
      val jsMain by getting {
        dependencies {
          implementation(npm("big.js", "5.2.2"))
        }
      }
    }
  }
}
