plugins {
  `java-library`
  kotlin("multiplatform")
}

configureMppDefaults()

kotlin {
  sourceSets {
    val commonMain by getting {
      dependencies {
        api(project(":apollo-api"))
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
