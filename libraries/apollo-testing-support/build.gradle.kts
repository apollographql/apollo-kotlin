plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("apollo.library")
}

apolloLibrary {
  mpp {
    withLinux.set(false)
  }
}

kotlin {
  sourceSets {
    val commonMain by getting {
      dependencies {
        api(project(":libraries:apollo-api"))
        api(project(":libraries:apollo-runtime"))
        api(project(":libraries:apollo-mockserver"))
        api(golatac.lib("kotlinx.coroutines"))
        implementation(golatac.lib("atomicfu")) {
          because("We need locks in TestNetworkTransportHandler (we don't use the gradle plugin rewrite)")
        }
        implementation(golatac.lib("kotlinx.coroutines.test"))
      }
    }

    val jvmTest by getting {
      dependencies {
        implementation(golatac.lib("truth"))
      }
    }

    val jsMain by getting {
      dependencies {
        implementation(golatac.lib("kotlinx.nodejs"))
        implementation(golatac.lib("kotlin.test.js"))
        api(okioNodeJs())
      }
    }
    val jsTest by getting {
      dependencies {
        implementation(golatac.lib("kotlin.test.js"))
      }
    }
  }
}
