plugins {
  kotlin("multiplatform")
}

configureMppDefaults(withJs = false)

kotlin {
  sourceSets {
    val commonMain by getting {
      dependencies {
        api(groovy.util.Eval.x(project, "x.dep.okio"))
        implementation(groovy.util.Eval.x(project, "x.dep.atomicfu").toString()) {
          because("We need locks for native (we don't use the gradle plugin rewrite)")
        }
      }
    }

    val jvmMain by getting {
      dependencies {
        implementation(groovy.util.Eval.x(project, "x.dep.okHttp.mockWebServer4"))
      }
    }

    val commonTest by getting {
      dependencies {
        implementation(projects.apolloTestingSupport) {
          because("runWithMainLoop")
        }
        implementation(projects.apolloRuntime) {
          because("We need HttpEngine for SocketTest")
        }
      }
    }
  }
}

