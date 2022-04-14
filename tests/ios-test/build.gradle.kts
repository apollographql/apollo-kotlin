plugins {
  id("com.apollographql.apollo3")
  id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
  ios()
  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation("com.apollographql.apollo3:apollo-runtime")
        implementation("com.apollographql.apollo3:apollo-mockserver")
      }
    }

    val commonTest by getting {
      dependencies {
        implementation(kotlin("test-junit"))
        implementation("com.apollographql.apollo3:apollo-testing-support")
      }
    }
  }
}

apollo {
  packageName.set("ios.test")
}
