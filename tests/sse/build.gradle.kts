plugins {
  id("org.jetbrains.kotlin.multiplatform")
}

configureMppTestsDefaults()

kotlin {
  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation("com.apollographql.apollo3:apollo-runtime")
      }
    }

    val commonTest by getting {
      dependencies {
        implementation("com.apollographql.apollo3:apollo-testing-support")
      }
    }

    val jvmTest by getting {
      dependencies {
        implementation(projects.sampleServer)
      }
    }
  }
}
