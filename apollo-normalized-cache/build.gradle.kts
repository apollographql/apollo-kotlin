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
        api(project(":apollo-normalized-cache-api"))
        api(groovy.util.Eval.x(project, "x.dep.kotlin.coroutines"))
      }
    }

    val jvmMain by getting {
      dependencies {
        implementation(groovy.util.Eval.x(project, "x.dep.cache"))
      }
    }

    val jvmTest by getting {
      dependencies {
        implementation(groovy.util.Eval.x(project, "x.dep.truth"))
      }
    }
  }
}
