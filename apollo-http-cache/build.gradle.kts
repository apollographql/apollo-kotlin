plugins {
  kotlin("jvm")
}

dependencies {
  api(groovy.util.Eval.x(project, "x.dep.okHttp.okHttp"))
  api(projects.apolloApi)
  api(projects.apolloRuntime)
  implementation(groovy.util.Eval.x(project, "x.dep.moshi.moshi"))
  implementation(groovy.util.Eval.x(project, "x.dep.kotlinxdatetime"))

  testImplementation(projects.apolloMockserver)
  testImplementation(kotlin("test-junit"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.truth"))
}

