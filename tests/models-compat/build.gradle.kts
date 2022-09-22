import com.apollographql.apollo3.compiler.MODELS_COMPAT

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
  service("service") {
    srcDir(file("../models-fixtures/graphql"))
    packageName.set("codegen.models")
    generateFragmentImplementations.set(true)
    generateDataBuilders.set(true)
    codegenModels.set("compat")
  }

  file("src/commonTest/kotlin/test").listFiles()!!
      .filter { it.isDirectory }
      .forEach {
        service(it.name) {
          srcDir(it)
          packageName.set(it.name)
          codegenModels.set(MODELS_COMPAT)
          generateDataBuilders.set(true)
        }
      }
}
