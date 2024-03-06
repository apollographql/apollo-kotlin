plugins {
  id("org.jetbrains.kotlin.multiplatform")
}

apolloLibrary(
    javaModuleName = "com.apollographql.apollo3.network.websocket",
    withLinux = false,
    publish = false
)

kotlin {
  sourceSets {
    findByName("commonMain")?.apply {
      dependencies {
        api(project(":apollo-runtime"))
        api(project(":apollo-mpp-utils"))
        api(libs.atomicfu.library)
      }
    }

    findByName("commonTest")?.apply {
      dependencies {
        implementation(project(":apollo-mockserver"))
        implementation(project(":apollo-testing-support")) {
          because("runTest")
        }
      }
    }

    findByName("jsMain")?.apply {
      dependencies {
        /**
         * WebSocket implementation for node
         */
        api(npm("ws", libs.versions.node.ws.get()))
        /**
         * Kotlin Node declarations
         *
         * The situation is a bit weird because jsMain has both browser and node dependencies but
         * there is not much we can do about it
         * See https://youtrack.jetbrains.com/issue/KT-47038
         */
        implementation(libs.kotlin.node)
      }
    }
  }
}


