plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("com.apollographql.apollo")
}

apolloTest(
    withJvm = false,
    appleTargets = emptySet()
)

kotlin {
  sourceSets {
    findByName("commonMain")?.apply {
      dependencies {
        implementation(libs.apollo.runtime)
      }
    }

    findByName("commonTest")?.apply {
      dependencies {
        implementation(libs.apollo.testingsupport.internal)
        implementation(libs.apollo.mockserver)
      }
    }
  }
}

apollo {
  service("service") {
    packageName.set("js.test")
  }
}
