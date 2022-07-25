plugins {
  id("com.apollographql.apollo3")
  kotlin("multiplatform")
}

kotlin {
  configureAppleTargets("macosX64", "macosArm64")

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation("com.apollographql.apollo3:apollo-runtime")
      }
    }

    val commonTest by getting {
      dependencies {
        implementation(kotlin("test-common"))
        implementation(kotlin("test-annotations-common"))
      }
    }
  }
}

apollo {
  packageName.set("test")
}

