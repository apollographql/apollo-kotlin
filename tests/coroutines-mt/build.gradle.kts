plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("apollo.test")
  id("com.apollographql.apollo3")
}

apolloTest {
  mpp {
    withJs.set(false)
    withJvm.set(false)
  }
}

kotlin {
  sourceSets {
    findByName("commonMain")?.apply {
      dependencies {
        implementation(golatac.lib("apollo.testingsupport"))
        implementation(golatac.lib("apollo.mockserver"))
        implementation(golatac.lib("apollo.normalizedcache"))
        implementation(golatac.lib("apollo.mpputils"))
        implementation(golatac.lib("apollo.runtime"))
      }
    }
  }
}

configurations {
  all {
    resolutionStrategy {
      force("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.3-native-mt")
      force("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.3")
    }
  }
}

apollo {
  service("myService") {
    packageName.set("macos.app")
    generateDataBuilders.set(true)
  }
}
