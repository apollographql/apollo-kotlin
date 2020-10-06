plugins {
  kotlin("jvm")
}

dependencies {
  compileOnly(groovy.util.Eval.x(project, "x.dep.jetbrainsAnnotations"))

  api(groovy.util.Eval.x(project, "x.dep.okHttp.okHttp"))
  api(project(":apollo-api"))
  api(project(":apollo-http-cache-api"))

  testImplementation(groovy.util.Eval.x(project, "x.dep.junit"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.truth"))
}

