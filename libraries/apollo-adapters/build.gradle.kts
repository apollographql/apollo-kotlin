plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("apollo.library")
}

apolloLibrary {
  javaModuleName("com.apollographql.apollo3.adapter")
  mpp {
    withLinux.set(false)
  }
}

kotlin {
  sourceSets {
    findByName("commonMain")?.apply {
      dependencies {
        api(project(":apollo-api"))
        api(golatac.lib("kotlinx.datetime"))
      }
    }
    findByName("jsMain")?.apply {
      dependencies {
        implementation(npm("big.js", "5.2.2"))
      }
    }
  }
}
