plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("apollo.test")
  id("com.apollographql.apollo3")
}

apolloTest {
  mpp {}
}

kotlin {
  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(libs.apollo.runtime)
        implementation(libs.apollo.normalizedcache)
        implementation(libs.apollo.adapters)
      }
    }

    val commonTest by getting {
      dependencies {
        implementation(libs.apollo.testingsupport)
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
