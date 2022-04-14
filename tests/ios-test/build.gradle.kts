plugins {
  id("com.apollographql.apollo3")
  id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
  // Workaround for https://youtrack.jetbrains.com/issue/KT-51970
  jvm()
  ios()
  sourceSets {
    val commonMain by getting {
      dependencies {
        // Workaround for something else (but what?)
        implementation("com.apollographql.apollo3:apollo-api")
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