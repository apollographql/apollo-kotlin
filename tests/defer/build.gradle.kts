plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("com.apollographql.apollo3")
}

configureMppTestsDefaults()

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
        implementation("com.apollographql.apollo3:apollo-runtime")
        implementation("com.apollographql.apollo3:apollo-normalized-cache")
      }
    }

    val commonTest by getting {
      dependencies {
        implementation("com.apollographql.apollo3:apollo-mockserver")
        implementation("com.apollographql.apollo3:apollo-testing-support")
        implementation(groovy.util.Eval.x(project, "x.dep.kotlinJunit"))
      }
    }

    val jsTest by getting {
      dependencies {
        implementation(npm("graphql", "16.0.0-experimental-stream-defer.5"))
        implementation(npm("graphql-helix", "1.12.0"))
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
