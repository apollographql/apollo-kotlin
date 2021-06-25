plugins {
  id("com.apollographql.apollo3")
  id("org.jetbrains.kotlin.multiplatform")
}

configureMppTestsDefaults()

kotlin {
  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation("com.apollographql.apollo3:apollo-runtime")
        implementation("com.apollographql.apollo3:apollo-cache-interceptor")
        implementation("com.apollographql.apollo3:apollo-adapters")
      }
    }

    val commonTest by getting {
      dependencies {
        implementation("com.apollographql.apollo3:apollo-testing-support")
      }
    }
  }
}

apollo {
  service("service") {
    srcDir(file("../models-fixtures/graphql"))
    packageName.set("codegen.models")
    generateFragmentImplementations.set(true)
    codegenModels.set("compat")
  }

  file("src/commonTest/kotlin/test").listFiles()!!
      .filter { it.isDirectory }
      .forEach {
        service(it.name) {
          srcDir(it)
          packageName.set(it.name)
        }
      }
}