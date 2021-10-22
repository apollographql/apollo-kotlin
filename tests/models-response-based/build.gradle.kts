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
        implementation("com.apollographql.apollo3:apollo-normalized-cache")
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
  srcDir(file("../models-fixtures/graphql"))
  packageName.set("codegen.models")
  generateFragmentImplementations.set(true)
  generateTestBuilders.set(true)
  customScalarsMapping.put("Date", "kotlin.Long")
  codegenModels.set("responseBased")
}