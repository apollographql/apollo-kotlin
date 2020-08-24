plugins {
  kotlin("multiplatform")
}

kotlin {
  jvm {
    withJava()
  }

  sourceSets {
    val jvmMain by getting {
      dependencies {
        api(project(":apollo-api"))
        api(project(":apollo-normalized-cache"))
        api(project(":apollo-http-cache-api"))
        api(groovy.util.Eval.x(project, "x.dep.okHttp.okHttp"))
      }
    }
    val jvmTest by getting {
      dependencies {
        api(groovy.util.Eval.x(project, "x.dep.mockito"))
        implementation(groovy.util.Eval.x(project, "x.dep.junit"))
        implementation(groovy.util.Eval.x(project, "x.dep.truth"))
        implementation(groovy.util.Eval.x(project, "x.dep.okHttp.mockWebServer"))
        implementation(project(":apollo-rx2-support"))
      }
    }
  }
}

tasks.withType<Javadoc> {
  options.encoding = "UTF-8"
}
