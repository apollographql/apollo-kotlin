plugins {
  id("apollo.library.multiplatform")
}

configureMppDefaults(withLinux = false)

kotlin {
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

apolloConvention {
  javaModuleName.set("com.apollographql.apollo3.adapter")
}
