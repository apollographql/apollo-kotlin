plugins {
  kotlin("jvm")
}

metalava {
  hiddenPackages += setOf("com.apollographql.apollo3.internal")
}

dependencies {
  api(project(":apollo-api"))
  api(project(":deprecated-apollo-http-cache-api"))
  api(project(":apollo-normalized-cache-common"))
  api(project(":deprecated-apollo-runtime-common"))
  api(groovy.util.Eval.x(project, "x.dep.okHttp.okHttp"))
  implementation(groovy.util.Eval.x(project, "x.dep.kotlin.coroutines"))

  testImplementation(groovy.util.Eval.x(project, "x.dep.junit"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.truth"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.okHttp.mockWebServer"))
  testImplementation(project(":deprecated-apollo-rx2-support"))
}
