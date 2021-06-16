plugins {
  kotlin("jvm")
}

dependencies {
  implementation(project(":apollo-api"))
  api(groovy.util.Eval.x(project, "x.dep.rx.java3"))
  api(project(":deprecated-apollo-runtime"))
}
