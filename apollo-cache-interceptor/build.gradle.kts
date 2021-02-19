plugins {
  kotlin("multiplatform")
}

configureMppDefaults(withJs = false)

kotlin {
  sourceSets {
    val commonMain by getting {
      dependencies {
        api(project(":apollo-api"))
        api(project(":apollo-runtime-kotlin"))
        api(project(":apollo-normalized-cache"))
        api(groovy.util.Eval.x(project, "x.dep.kotlin.coroutines"))
      }
    }

    val commonTest by getting {
      dependencies {
        implementation(project(":apollo-testing-support"))
      }
    }

    val jvmTest by getting {
      dependencies {
        implementation(groovy.util.Eval.x(project, "x.dep.truth"))
      }
    }
  }
}

