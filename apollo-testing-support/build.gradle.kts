plugins {
  kotlin("multiplatform")
}

// testing support is only used for tests and therefore doesn't need Browser targets
// So we configure JS manually
configureMppDefaults(withJs = false)

kotlin {
  js(IR) {
    useCommonJs()
    nodejs()
  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        api(projects.apolloApi)
        api(projects.apolloNormalizedCacheApi)
        api(projects.apolloRuntime)
        api(projects.apolloMockserver)
        api(groovy.util.Eval.x(project, "x.dep.kotlin.coroutines"))
      }
    }

    val jvmTest by getting {
      dependencies {
        implementation(groovy.util.Eval.x(project, "x.dep.truth"))
      }
    }

    val jsMain by getting {
      dependencies {
        implementation(groovy.util.Eval.x(project, "x.dep.kotlin.nodejs"))
        implementation(kotlin("test-js"))
      }
    }
    val jsTest by getting {
      dependencies {
        implementation(kotlin("test-js"))
      }
    }
  }
}