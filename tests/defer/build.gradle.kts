plugins {
  id("apollo.test.multiplatform")
}

apolloConvention {
  kotlin {
    /**
     * Extra target to test the java codegen
     */
    jvm("javaCodegen") {
      withJava()
    }

    sourceSets {
      val commonMain by getting {
        dependencies {
          implementation(libs.apollo.runtime)
          implementation(libs.apollo.normalizedcache)
        }
      }

      val commonTest by getting {
        dependencies {
          implementation(libs.apollo.mockserver)
          implementation(libs.apollo.testingsupport)
        }
      }

      val jsTest by getting {
        dependencies {
          implementation(npm("graphql-helix", "1.12.0"))
          implementation(npm("express", "4.17.3"))
          implementation(npm("ws", "8.2.2"))
          implementation(npm("graphql-ws", "5.5.0"))
          // Depend on a more recent 'canary' version of graphql-js (version graphql-helix depends on by default is older).
          // This corresponds to this PR: https://github.com/graphql/graphql-js/pull/2839/
          implementation(npm("graphql", "canary-pr-2839"))
        }
      }

      val jvmTest by getting {
        dependencies {
          implementation(libs.apollo.httpCache)
        }
      }
    }
  }
}

apollo {
  service("kotlin") {
    packageName.set("defer")
    generateKotlinModels.set(true)
    configureConnection(true)
  }
  service("java") {
    packageName.set("defer")
    generateKotlinModels.set(false)
    configureConnection(false)
  }
}

fun com.apollographql.apollo3.gradle.api.Service.configureConnection(generateKotlinModels: Boolean) {
  outputDirConnection {
    if (System.getProperty("idea.sync.active") == null) {
      if (generateKotlinModels) {
        connectToKotlinSourceSet("jvmTest")
        connectToKotlinSourceSet("jsTest")
        connectToKotlinSourceSet("appleTest")
      } else {
        connectToJavaSourceSet("main")
      }
    } else {
      // For autocomplete to work
      connectToKotlinSourceSet("commonTest")
    }
  }
}
