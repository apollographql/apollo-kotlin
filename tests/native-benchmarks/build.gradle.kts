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
        implementation(golatac.lib("apollo.runtime"))
        implementation(golatac.lib("apollo.normalizedcache"))
      }
    }

    findByName("commonTest")?.apply {
      dependencies {
        implementation(golatac.lib("apollo.testingsupport"))
        implementation(golatac.lib("apollo.mockserver"))
        implementation(golatac.lib("apollo.mpputils"))
      }
    }
  }
}

apollo {
  service("service") {
    packageName.set("benchmarks")
    generateDataBuilders.set(true)
  }
}
