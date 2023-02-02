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
    findByName("commonMain")?.apply {
      dependencies {
        api(project(":apollo-api"))
        api(project(":apollo-mpp-utils"))
        api(okio())
        api(golatac.lib("uuid"))
        api(golatac.lib("kotlinx.coroutines"))
      }
    }

    findByName("commonTest")?.apply {
      dependencies {
        implementation(project(":apollo-mockserver"))
        implementation(project(":apollo-testing-support")) {
          because("runTest")
          // We have a circular dependency here that creates a warning in JS
          // w: duplicate library name: com.apollographql.apollo3:apollo-mockserver
          // See https://youtrack.jetbrains.com/issue/KT-51110
          // We should probably remove this circular dependency but for the time being, just use excludes
          exclude(group =  "com.apollographql.apollo3", module = "apollo-runtime")
        }
      }
    }

    findByName("jvmMain")?.apply {
      dependencies {
        api(golatac.lib("okhttp"))
      }
    }

    findByName("jsMain")?.apply {
      dependencies {
        api(golatac.lib("ktor.client.js"))
      }
    }

    findByName("appleMain")?.apply {
      dependencies {
      }
    }

    findByName("jvmTest")?.apply {
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
