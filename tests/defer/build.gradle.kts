plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("com.apollographql.apollo")
}

apolloTest()

kotlin {
  sourceSets {
    findByName("commonMain")?.apply {
      dependencies {
        implementation(libs.apollo.runtime)
        implementation(libs.apollo.normalizedcache)
      }
    }

    findByName("commonTest")?.apply {
      dependencies {
        implementation(libs.apollo.normalizedcache)
        implementation(libs.apollo.testingsupport)
        implementation(libs.apollo.mockserver)
      }
    }

    findByName("jsTest")?.apply {
      dependencies {
        implementation(npm("graphql-helix", "1.13.0"))
        implementation(npm("express", "4.17.3"))
        implementation(npm("ws", "8.2.2"))
        implementation(npm("graphql-ws", "5.5.0"))
        // Depend on a more recent 'canary' version of graphql-js (version graphql-helix depends on by default is older).
        // This corresponds to this PR: https://github.com/graphql/graphql-js/pull/2839/
        implementation(npm("graphql", "canary-pr-2839"))
      }
    }

    findByName("jvmTest")?.apply {
      dependencies {
        implementation(libs.apollo.httpCache)
      }
    }
  }
}

fun configureApollo(generateKotlinModels: Boolean) {
  val extra = if (generateKotlinModels) "kotlin" else "java"

  apollo {
    service("base-$extra") {
      packageName.set("defer")
      this.generateKotlinModels.set(generateKotlinModels)
      srcDir("src/commonMain/graphql/base")
      configureConnection(generateKotlinModels)
    }
    service("supergraph-$extra") {
      packageName.set("supergraph")
      srcDir("src/commonMain/graphql/supergraph")
      this.addTypename.set("ifAbstract")
      this.generateKotlinModels.set(generateKotlinModels)
      configureConnection(generateKotlinModels)
    }
  }
}

configureApollo(true)
if (System.getProperty("idea.sync.active") == null) {
  registerJavaCodegenTestTask()
  configureApollo(false)
}

fun com.apollographql.apollo.gradle.api.Service.configureConnection(generateKotlinModels: Boolean) {
  outputDirConnection {
    if (generateKotlinModels) {
      connectToKotlinSourceSet("commonTest")
    } else {
      connectToJavaSourceSet("javaCodegenTest")
    }
  }
}

tasks.withType(AbstractTestTask::class.java) {
  // Run the defer with Router tests only from a specific CI job
  val runDeferWithRouterTests = System.getenv("DEFER_WITH_ROUTER_TESTS").toBoolean()
  if (runDeferWithRouterTests) {
    filter.setIncludePatterns("test.DeferWithRouterTest")
  } else {
    filter.setExcludePatterns("test.DeferWithRouterTest")
  }
}

