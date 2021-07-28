plugins {
  kotlin("multiplatform")
}

configureMppDefaults(withJs = false)

kotlin {
  sourceSets {
    val commonMain by getting {
      dependencies {
        api(project(":apollo-api"))
        api(project(":apollo-mpp-utils"))
        api(project(":apollo-runtime"))
        api(project(":apollo-normalized-cache-common"))
        api(groovy.util.Eval.x(project, "x.dep.kotlin.coroutines"))
      }
    }
  }
}
