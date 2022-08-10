plugins {
  id("apollo.library.multiplatform")
}

apolloConvention {
  javaModuleName.set("com.apollographql.apollo3.annotations")

  kotlin {
    sourceSets {
      val commonMain by getting {
        dependencies {
          api(libs.kotlin.stdlib)
        }
      }

      val jsMain by getting {
        dependencies {
          // See https://youtrack.jetbrains.com/issue/KT-53471
          api(libs.kotlin.stdlib.js)
        }
      }
    }
  }
}
