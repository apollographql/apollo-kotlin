plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("apollo.test")
}

apolloTest {
  mpp {}
}

kotlin {
  sourceSets {
    findByName("commonMain")?.apply {
      dependencies {
        implementation(libs.apollo.runtime)
        implementation(libs.apollo.engine.ktor)
      }
    }

    findByName("commonTest")?.apply {
      dependencies {
        implementation(libs.apollo.mockserver)
        implementation(libs.apollo.testingsupport)
      }
    }

    findByName("jvmTest")?.apply {
      dependencies {
        implementation(project(":sample-server"))
      }
    }
  }
}
