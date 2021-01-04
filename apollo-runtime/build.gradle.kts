plugins {
  kotlin("jvm")
}

metalava {
  hiddenPackages += setOf("com.apollographql.apollo.internal")
}

dependencies {
  api(project(":apollo-api"))
  api(project(":apollo-normalized-cache"))
  api(project(":apollo-http-cache-api"))
  api(groovy.util.Eval.x(project, "x.dep.okHttp.okHttp"))

  testImplementation(groovy.util.Eval.x(project, "x.dep.junit"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.truth"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.okHttp.mockWebServer"))
  testImplementation(project(":apollo-rx2-support"))
}
