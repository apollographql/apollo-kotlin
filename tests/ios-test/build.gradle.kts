plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("apollo.test")
  id("com.apollographql.apollo3")
}

apolloTest {
  mpp {
    withJs.set(false)
    withJvm.set(false)
    appleTargets.set(setOf("iosArm64", "iosX64"))
  }
}

kotlin {
  sourceSets {
    findByName("commonMain")?.apply {
      dependencies {
        implementation(golatac.lib("apollo.runtime"))
        implementation(golatac.lib("apollo.mockserver"))
      }
    }

    findByName("commonTest")?.apply {
      dependencies {
        implementation(golatac.lib("apollo.testingsupport"))
      }
    }
  }
}

apollo {
  service("service") {
    packageName.set("ios.test")
    generateDataBuilders.set(true)
  }
}
