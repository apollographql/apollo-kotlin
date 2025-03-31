plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("com.apollographql.apollo")
}

apolloTest(withJs = false, appleTargets = setOf("iosSimulatorArm64"))

kotlin {
  iosSimulatorArm64 {
    binaries {
      framework {
        export(libs.apollo.api)
        binaryOption("bundleId", "shared_framework")
      }
    }
  }
  sourceSets {
    findByName("commonMain")?.apply {
      dependencies {
        api(libs.apollo.runtime)
        implementation(libs.apollo.mockserver)
      }
    }

    findByName("commonTest")?.apply {
      dependencies {
      }
    }
  }
}

apollo {
  service("service") {
    packageName.set("com.example")
  }
}