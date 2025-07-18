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
        implementation(libs.apollo.testingsupport.internal)
        implementation(libs.apollo.mockserver)
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

apollo {
  service("noTypename") {
    packageName.set("defer.notypename")
    srcDir("src/commonMain/graphql/noTypename")
    addTypename.set("ifPolymorphic")
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
      connectToJavaSourceSet("jvmJavaCodegenTest")
    }
  }
}

tasks.withType(AbstractTestTask::class.java) {
  // Run the defer with Router and defer with Apollo Server tests only from a specific CI job
  val runDeferWithRouterTests = System.getenv("DEFER_WITH_ROUTER_TESTS").toBoolean()
  val runDeferWithApolloServerTests = true
  filter.setIncludePatterns(*buildList {
    if (runDeferWithRouterTests) add("test.DeferWithRouterTest")
    if (runDeferWithApolloServerTests) add("test.DeferWithApolloServerTest")
  }.toTypedArray())
  filter.setExcludePatterns(*buildList {
    if (!runDeferWithRouterTests) add("test.DeferWithRouterTest")
    if (!runDeferWithApolloServerTests) add("test.DeferWithApolloServerTest")
  }.toTypedArray())
}
