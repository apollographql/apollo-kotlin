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
    val commonMain by getting {
      dependencies {
        api(project(":apollo-api"))
        api(golatac.lib("kotlinx.datetime"))
      }
    }
    val jsMain by getting {
      dependencies {
        implementation(npm("big.js", "5.2.2"))
      }
    }
    val jvmMain by getting {
      dependencies {
        compileOnly(golatac.lib("guava.jre"))
      }
    }
  }
}
