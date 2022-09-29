plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("apollo.library")
}

apolloLibrary {
  javaModuleName("com.apollographql.apollo3.runtime")
  mpp {
    withLinux.set(false)
  }
}

kotlin {
  sourceSets {
    val commonMain by getting {
      dependencies {
        api(project(":libraries:apollo-api"))
        api(project(":libraries:apollo-mpp-utils"))
        api(okio())
        api(golatac.lib("uuid"))
        api(golatac.lib("kotlinx.coroutines"))
      }
    }

    val commonTest by getting {
      dependencies {
        implementation(project(":libraries:apollo-mockserver"))
        implementation(project(":libraries:apollo-testing-support"))
      }
    }

    val jvmMain by getting {
      dependencies {
        api(golatac.lib("okhttp"))
      }
    }

    val jsMain by getting {
      dependencies {
        api(golatac.lib("ktor.client.js"))
      }
    }

    val appleMain by getting {
      dependencies {
      }
    }

    val jvmTest by getting {
      dependencies {
        implementation(golatac.lib("kotlin.test.junit"))
        implementation(golatac.lib("truth"))
        implementation(golatac.lib("okhttp"))
      }
    }
  }
}

tasks.register("iOSSimTest") {
  dependsOn("iosSimTestBinaries")
  doLast {
    val binary = kotlin.targets.getByName<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>("iosSim").binaries.getTest("DEBUG").outputFile
    exec {
      commandLine = listOf("xcrun", "simctl", "spawn", "iPhone 8", binary.absolutePath)
    }
  }
}
