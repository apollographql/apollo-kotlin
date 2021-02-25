plugins {
  kotlin("multiplatform")
}

configureMppDefaults()

kotlin {
  sourceSets {
    val commonMain by getting {
      dependencies {
        api(groovy.util.Eval.x(project, "x.dep.okio"))
        api(groovy.util.Eval.x(project, "x.dep.uuid"))
      }
    }

    val jsMain by getting {
      dependencies {
        implementation(npm("big.js", "5.2.2"))
      }
    }

    val jvmTest by getting {
      dependencies {
        implementation(groovy.util.Eval.x(project, "x.dep.truth"))
        implementation(groovy.util.Eval.x(project, "x.dep.okHttp.okHttp"))
      }
    }
  }
}

metalava {
  hiddenPackages += setOf("com.apollographql.apollo3.api.internal")
}
