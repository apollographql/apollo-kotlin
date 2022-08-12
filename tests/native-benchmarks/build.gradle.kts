plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("apollo.test")
  id("com.apollographql.apollo3")
}

apolloTest {
  mpp {
    withJs.set(false)
    withJvm.set(false)
  }
}

kotlin {
  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(libs.apollo.runtime)
        implementation(libs.apollo.normalizedcache)
      }
    }

    val commonTest by getting {
      dependencies {
        implementation(libs.apollo.testingsupport)
        implementation(libs.apollo.mockserver)
        implementation(libs.apollo.mpputils)
      }
    }
  }
}

apollo {
  packageName.set("benchmarks")
  generateDataBuilders.set(true)
}
