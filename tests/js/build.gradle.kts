plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("apollo.test")
  id("com.apollographql.apollo3")
}

apolloTest {
  mpp {
    withJvm.set(false)
    appleTargets.set(emptySet())
  }
}

kotlin {
  sourceSets {
    findByName("commonMain")?.apply {
      dependencies {
        implementation(golatac.lib("apollo.runtime"))
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
    packageName.set("js.test")
  }
}
