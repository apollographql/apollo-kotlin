plugins {
  id(libs.plugins.apollo.get().toString())
  id(libs.plugins.kotlin.multiplatform.get().toString())
}

configureMppTestsDefaults()

kotlin {
  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(libs.apollo.runtime)
        implementation(libs.apollo.normalizedCache)
        implementation(libs.apollo.adapters)
      }
    }

    val commonTest by getting {
      dependencies {
        implementation(libs.apollo.testingSupport)
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
  sealedClassesForEnumsMatching.set(setOf("StarshipType"))
}
