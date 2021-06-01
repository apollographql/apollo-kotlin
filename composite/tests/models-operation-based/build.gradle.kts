plugins {
  id("com.apollographql.apollo3")
  id("org.jetbrains.kotlin.multiplatform")
}

configureMppTestsDefaults()

kotlin {
  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation("com.apollographql.apollo3:apollo-runtime-kotlin")
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
  addGraphqlDirectory(file("../models-fixtures/graphql"))
  rootPackageName.set("codegen.models")
  generateFragmentImplementations.set(true)
  codegenModels.set("operationBased")
}