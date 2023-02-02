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
        api(project(":apollo-annotations"))
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
        implementation(project(":apollo-testing-support")) {
          because("runTest")
          // We have a circular dependency here that creates a warning in JS
          // w: duplicate library name: com.apollographql.apollo3:apollo-mockserver
          // See https://youtrack.jetbrains.com/issue/KT-51110
          // We should probably remove this circular dependency but for the time being, just use excludes
          exclude(group =  "com.apollographql.apollo3", module = "apollo-mockserver")
        }
        implementation(project(":apollo-runtime")) {
          because("We need HttpEngine for SocketTest")
        }
      }
    }
  }
}

