plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("apollo.test")
  id("com.apollographql.apollo3")
}

apolloTest {
  mpp {
    withJs.set(false)
  }
}

kotlin {
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

apollo {
  packageName.set("sqlite")
}
