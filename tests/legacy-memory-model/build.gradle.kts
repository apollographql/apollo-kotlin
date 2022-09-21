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
    val commonMain by getting {
      dependencies {
        implementation(golatac.lib("apollo.runtime"))
      }
    }
  }
}

apollo {
  packageName.set("test")
}
