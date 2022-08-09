plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("com.apollographql.apollo3")
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
  service("pagination") {
    packageName.set("pagination")
    sourceFolder.set("pagination")
    generateTestBuilders.set(true)
  }
  service("embed") {
    packageName.set("embed")
    sourceFolder.set("embed")
  }
}
