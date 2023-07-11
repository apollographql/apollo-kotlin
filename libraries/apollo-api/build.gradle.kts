plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("apollo.library")
}

apolloLibrary {
  javaModuleName("com.apollographql.apollo3.api")
  mpp {}
}

kotlin {
  sourceSets {
    findByName("commonMain")?.apply {
      dependencies {
        api(libs.okio)
        api(libs.uuid)
        api(project(":apollo-annotations"))
      }
    }

    findByName("commonTest")?.apply {
      dependencies {
        //implementation(libs.kotlin.test.junit)
        //implementation("org.jetbrains.kotlin:kotlin-test")
        implementation(libs.kotlin.test.asProvider().get().toString())
      }
    }
  }
}

