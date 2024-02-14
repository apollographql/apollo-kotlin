plugins {
  id("org.jetbrains.kotlin.multiplatform")
}

apolloLibrary(
  javaModuleName = "com.apollographql.apollo3.cache.normalized.api",
    withLinux = false,
)

kotlin {
  sourceSets {
    findByName("commonMain")?.apply {
      dependencies {
        api(project(":apollo-api"))
        api(project(":apollo-mpp-utils"))
        implementation(libs.okio)
        api(libs.uuid)
        implementation(libs.atomicfu.get().toString()) {
          because("Use of ReentrantLock for Apple (we don't use the gradle plugin rewrite)")
        }
      }
    }

    findByName("jvmMain")?.apply {
      dependencies {
        implementation(libs.guava.jre)
      }
    }

    findByName("commonTest")?.apply {
      dependencies {
        implementation(project(":apollo-testing-support"))
      }
    }
  }
}
