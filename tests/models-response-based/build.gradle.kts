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
        implementation(golatac.lib("apollo.runtime"))
        implementation(golatac.lib("apollo.normalizedcache"))
        implementation(golatac.lib("apollo.adapters"))
      }
    }

    val commonTest by getting {
      dependencies {
        implementation(golatac.lib("apollo.testingsupport"))
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
