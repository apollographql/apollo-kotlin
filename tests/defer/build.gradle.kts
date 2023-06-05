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
   * Extra target to test the java codegen
   */
  jvm("javaCodegen") {
    withJava()
  }

  sourceSets {
    findByName("commonMain")?.apply {
      dependencies {
        implementation(golatac.lib("apollo.runtime"))
        implementation(golatac.lib("apollo.normalizedcache"))
      }
    }

    findByName("commonTest")?.apply {
      dependencies {
        implementation(golatac.lib("apollo.mockserver"))
        implementation(golatac.lib("apollo.testingsupport"))
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
        implementation(golatac.lib("apollo.httpCache"))
      }
    }
  }
}

apollo {
  service("kotlin") {
    packageName.set("defer")
    generateKotlinModels.set(true)
    sourceFolder.set("base")
    configureConnection(true)
  }
  service("java") {
    packageName.set("defer")
    generateKotlinModels.set(false)
    sourceFolder.set("base")
    configureConnection(false)
  }
  service("supergraph") {
    packageName.set("supergraph")
    sourceFolder.set("supergraph")
    this.addTypename.set("ifAbstract")

    configureConnection(true)
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

tasks.withType(AbstractTestTask::class.java) {
  // Run the defer with Router tests only from a a specific CI job
  val runDeferWithRouterTests = System.getenv("COM_APOLLOGRAPHQL_DEFER_WITH_ROUTER_TESTS").toBoolean()
  if (runDeferWithRouterTests) {
    filter.setIncludePatterns("test.DeferWithRouterTest")
  } else {
    filter.setExcludePatterns("test.DeferWithRouterTest")
  }
}

// See https://youtrack.jetbrains.com/issue/KT-56019
val myAttribute = Attribute.of("com.apollographql.test", String::class.java)

configurations.named("jvmApiElements").configure {
  attributes {
    attribute(myAttribute, "jvm")
  }
}

configurations.named("javaCodegenApiElements").configure {
  attributes {
    attribute(myAttribute, "java")
  }
}

configurations.named("jvmRuntimeElements").configure {
  attributes {
    attribute(myAttribute, "jvm-runtime")
  }
}

configurations.named("javaCodegenRuntimeElements").configure {
  attributes {
    attribute(myAttribute, "java-runtime")
  }
}

configurations.named("jvmSourcesElements").configure {
  attributes {
    attribute(myAttribute, "jvm")
  }
}

configurations.named("javaCodegenSourcesElements").configure {
  attributes {
    attribute(myAttribute, "java")
  }
}
