plugins {
  kotlin("jvm")
}

dependencies {
  api(groovy.util.Eval.x(project, "x.dep.okHttp.okHttp"))
  api(project(":apollo-api"))
  api(project(":apollo-runtime"))
  implementation(groovy.util.Eval.x(project, "x.dep.moshi.moshi"))
  implementation(groovy.util.Eval.x(project, "x.dep.kotlinxdatetime"))

  testImplementation(project(":apollo-mockserver"))
  testImplementation(kotlin("test-junit"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.truth"))
}

