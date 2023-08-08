plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("apollo.library")
}

apolloLibrary {
  javaModuleName("com.apollographql.apollo3.engine.ktor")
  mpp {
    withLinux.set(false)
  }
}

kotlin {
  sourceSets {
    findByName("commonMain")?.apply {
      dependencies {
        api(project(":apollo-runtime"))
        api(libs.ktor.client.core)
        api(libs.ktor.client.websockets)
      }
    }

    findByName("commonTest")?.apply {
      dependencies {
        implementation(project(":apollo-mockserver"))
        implementation(project(":apollo-testing-support")) {
          because("runTest")
          // We have a circular dependency here that creates a warning in JS
          // w: duplicate library name: com.apollographql.apollo3:apollo-mockserver
          // See https://youtrack.jetbrains.com/issue/KT-51110
          // We should probably remove this circular dependency but for the time being, just use excludes
          exclude(group =  "com.apollographql.apollo3", module = "apollo-runtime")
        }
      }
    }

    findByName("jvmMain")?.apply {
      dependencies {
        api(libs.ktor.client.okhttp)
      }
    }

    findByName("appleMain")?.apply {
      dependencies {
        api(libs.ktor.client.darwin)
      }
    }
  }
}
