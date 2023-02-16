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
    findByName("commonMain")?.apply {
      dependencies {
        implementation(golatac.lib("apollo.runtime"))
        implementation(golatac.lib("apollo.normalizedcache"))
        implementation(golatac.lib("apollo.adapters"))
      }
    }

    findByName("commonTest")?.apply {
      dependencies {
        implementation(golatac.lib("apollo.testingsupport"))
      }
    }

    findByName("javaCodegenTest")?.apply {
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
}

configureApollo(true)
configureApollo(false)

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
