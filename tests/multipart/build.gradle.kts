plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("apollo.test")
  id("com.apollographql.apollo3")
}

apolloTest {
  mpp {}
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
  service("mockserver") {
    sourceFolder.set("mockserver")
    packageName.set("multipart")
  }
  service("router") {
    sourceFolder.set("router")
    packageName.set("router")
  }
}
