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
    findByName("commonMain")?.apply {
      dependencies {
        api(project(":apollo-api"))
        api(project(":apollo-runtime"))
        api(project(":apollo-mockserver"))
        api(golatac.lib("kotlinx.coroutines"))
        implementation(golatac.lib("atomicfu")) {
          because("We need locks in TestNetworkTransportHandler (we don't use the gradle plugin rewrite)")
        }
        implementation(golatac.lib("kotlinx.coroutines.test"))
      }
    }

    findByName("jvmTest")?.apply {
      dependencies {
        implementation(golatac.lib("truth"))
      }
    }

    findByName("jsMain")?.apply {
      dependencies {
        implementation(golatac.lib("kotlinx.nodejs"))
        implementation(golatac.lib("kotlin.test.js"))
        api(okioNodeJs())
      }
    }
    findByName("jsTest")?.apply {
      dependencies {
        implementation(golatac.lib("kotlin.test.js"))
      }
    }
  }
}
