plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("apollo.test")
  id("com.apollographql.apollo3")
}

apolloTest {
  mpp {
    withJs.set(false)
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
        implementation(golatac.lib("apollo.normalizedcache.incubating"))
        implementation(golatac.lib("apollo.normalizedcache.sqlite.incubating"))
      }
    }
  }
}

apollo {
  service("pagination") {
    packageName.set("pagination")
    sourceFolder.set("pagination")
    generateDataBuilders.set(true)
  }
  service("embed") {
    packageName.set("embed")
    sourceFolder.set("embed")
  }
}
