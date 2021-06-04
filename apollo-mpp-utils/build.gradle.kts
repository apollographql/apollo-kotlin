plugins {
  kotlin("multiplatform")
}

configureMppDefaults()

kotlin {
  sourceSets {
    val commonMain by getting {
      dependencies {
        api(groovy.util.Eval.x(project, "x.dep.kotlin.coroutines"))
      }
    }
  }
}

