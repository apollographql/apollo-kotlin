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
        api(groovy.util.Eval.x(project, "x.dep.kotlin.coroutines"))
      }
    }

    val jvmTest by getting {
      dependencies {
        implementation(groovy.util.Eval.x(project, "x.dep.truth"))
      }
    }
  }
}

