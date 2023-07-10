plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("apollo.library")
}

apolloLibrary {
  javaModuleName("com.apollographql.apollo3.annotations")
  mpp {}
}

kotlin {
  sourceSets {
    findByName("commonMain")?.apply {
      dependencies {
        api(libs.kotlin.stdlib)
        api(libs.jetbrains.annotations)
      }
    }

    findByName("jsMain")?.apply {
      dependencies {
        // See https://youtrack.jetbrains.com/issue/KT-53471
        api(libs.kotlin.stdlib.js)
      }
    }
  }
}
