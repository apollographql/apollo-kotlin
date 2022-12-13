plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("apollo.test")
  id("com.apollographql.apollo3")
}

apolloTest {
  mpp {
    withJs.set(false)
    withJvm.set(false)
    newMemoryManager.set(false)
  }
}

kotlin {
  sourceSets {
    findByName("commonMain")?.apply {
      dependencies {
        implementation(golatac.lib("apollo.runtime"))
      }
    }
  }
}

apollo {
  service("service") {
    packageName.set("test")
  }
}
