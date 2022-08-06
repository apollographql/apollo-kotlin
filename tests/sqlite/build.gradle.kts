plugins {
  id("com.apollographql.apollo3")
  id("org.jetbrains.kotlin.multiplatform")
}

configureMppTestsDefaults(withJs = false)

kotlin {
  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(libs.apollo.runtime)
      }
    }

    val commonTest by getting {
      dependencies {
        implementation(libs.apollo.testingSupport)
        implementation(libs.apollo.normalizedCache.incubating)
        implementation(libs.apollo.normalizedCache.sqlite.incubating)
      }
    }
  }
}

apollo {
  packageName.set("sqlite")
}
