plugins {
  `java-library`
  kotlin("multiplatform")
  id("kotlinx-atomicfu")
}

configureMppDefaults()

kotlin {
  sourceSets {
    val commonMain by getting {
      dependencies {
        api(project(":apollo-api"))
        api(project(":apollo-normalized-cache-api"))
        implementation(groovy.util.Eval.x(project, "x.dep.kotlin.atomic"))
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
