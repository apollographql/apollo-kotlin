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
        implementation(libs.apollo.testingsupport.internal)
        implementation(libs.apollo.mockserver)
      }
    }

    findByName("javaCodegenTest")?.apply {
      dependencies {
        // Add test-junit manually because configureMppTestsDefaults did not do it for us
        implementation(libs.kotlin.test.junit)
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

fun com.apollographql.apollo.gradle.api.Service.configureConnection(generateKotlinModels: Boolean) {
  outputDirConnection {
    if (generateKotlinModels) {
      connectToKotlinSourceSet("commonTest")
    } else {
      connectToJavaSourceSet("jvmJavaCodegenTest")
    }
  }
}

configureApollo(true)

if (System.getProperty("idea.sync.active") == null) {
  registerJavaCodegenTestTask()
  configureApollo(false)
}
