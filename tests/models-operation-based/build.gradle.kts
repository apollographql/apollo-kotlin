plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("apollo.test")
  id("com.apollographql.apollo3")
}

apolloTest {
  mpp {}
}

kotlin {
  /**
   * Extra target to test the java codegen. There will be 2 JVM tasks:
   * - compileKotlinJvm
   * - compileKotlinJavaCodegen
   */
  jvm("javaCodegen") {
    withJava()
  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(golatac.lib("apollo.runtime"))
        implementation(golatac.lib("apollo.normalizedcache"))
        implementation(golatac.lib("apollo.adapters"))
      }
    }

    val commonTest by getting {
      dependencies {
        implementation(golatac.lib("apollo.testingsupport"))
      }
    }

    val javaCodegenTest by getting {
      dependencies {
        // Add test-junit manually because configureMppTestsDefaults did not do it for us
        implementation(golatac.lib("kotlin.test.junit"))
      }
    }
  }
}


fun configureApollo(generateKotlinModels: Boolean) {
  val extra = if (generateKotlinModels) "kotlin" else "java"
  apollo {
    service("service-$extra") {
      srcDir(file("../models-fixtures/graphql"))
      packageName.set("codegen.models")
      generateFragmentImplementations.set(true)

      codegenModels.set("operationBased")
      this.generateTestBuilders.set(generateKotlinModels)
      this.generateKotlinModels.set(generateKotlinModels)
      configureConnection(generateKotlinModels)
    }
  }
}

fun com.apollographql.apollo3.gradle.api.Service.configureConnection(generateKotlinModels: Boolean) {
  outputDirConnection {
    if (System.getProperty("idea.sync.active") == null) {
      if (generateKotlinModels) {
        connectToKotlinSourceSet("jvmTest")
        connectToKotlinSourceSet("appleTest")
        connectToKotlinSourceSet("jsTest")
      } else {
        // For java, the source set is always called 'main'
        connectToJavaSourceSet("main")
      }
    } else {
      // For autocomplete to work
      connectToKotlinSourceSet("commonTest")
    }
  }
  testDirConnection {
    if (generateKotlinModels) {
      // Only connect to jvmTest, not commonTest
      connectToKotlinSourceSet("jvmTest")
    }
  }
}

configureApollo(true)
configureApollo(false)

