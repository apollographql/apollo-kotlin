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
    findByName("commonMain")?.apply {
      dependencies {
        api(project(":libraries:apollo-annotations"))
        api(okio())
        implementation(golatac.lib("atomicfu")) {
          because("We need locks for native (we don't use the gradle plugin rewrite)")
        }
        api(golatac.lib("kotlinx.coroutines"))
      }
    }

    findByName("jsMain")?.apply {
      dependencies {
        implementation(golatac.lib("kotlinx.nodejs"))
      }
    }

    findByName("commonTest")?.apply {
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

