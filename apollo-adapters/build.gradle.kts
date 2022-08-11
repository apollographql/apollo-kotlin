plugins {
  id("apollo.library")
  id("org.jetbrains.kotlin.multiplatform")
}

apolloLibrary {
  javaModuleName.set("com.apollographql.apollo3.adapter")
  mpp {
    withLinux.set(false)
  }
}

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
