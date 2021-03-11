plugins {
  `java-library`
  kotlin("multiplatform")
}

configureMppDefaults(withJs = false)

kotlin {
  sourceSets {
    val commonMain by getting {
      dependencies {
        api(project(":apollo-api"))
        api(groovy.util.Eval.x(project, "x.dep.kotlin.coroutines"))
      }
    }

    val jvmMain by getting {
      dependencies {
        api(groovy.util.Eval.x(project, "x.dep.okHttp.okHttp"))
      }
    }

    val commonTest by getting {
      dependencies {
        implementation(project(":apollo-testing-support"))
      }
    }
  }
}
