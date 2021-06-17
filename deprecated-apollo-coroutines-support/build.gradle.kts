plugins {
  kotlin("jvm")
}

dependencies {
  api(project(":deprecated-apollo-runtime"))
  api(project(":apollo-api"))
  api(groovy.util.Eval.x(project, "x.dep.kotlin.coroutines"))

  testImplementation(groovy.util.Eval.x(project, "x.dep.junit"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.truth"))
}

