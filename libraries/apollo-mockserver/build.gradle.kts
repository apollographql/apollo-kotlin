plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("apollo.library")
}

apolloLibrary {
  javaModuleName("com.apollographql.apollo3.mockserver")
  mpp {
    withLinux.set(false)
  }
}

kotlin {
  sourceSets {
    val commonMain by getting {
      dependencies {
        api(project(":libraries:apollo-annotations"))
        api(okio())
        implementation(golatac.lib("atomicfu")) {
          because("We need locks for native (we don't use the gradle plugin rewrite)")
        }
        implementation(golatac.lib("kotlinx.coroutines"))
      }
    }

    val jsMain by getting {
      dependencies {
        implementation(golatac.lib("kotlinx.nodejs"))
      }
    }

    val commonTest by getting {
      dependencies {
        implementation(project(":libraries:apollo-testing-support")) {
          because("runTest")
        }
        implementation(project(":libraries:apollo-runtime")) {
          because("We need HttpEngine for SocketTest")
        }
      }
    }
  }
}

