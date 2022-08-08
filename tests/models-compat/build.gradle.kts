import com.apollographql.apollo3.compiler.MODELS_COMPAT

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
          codegenModels.set(MODELS_COMPAT)
        }
      }
}
