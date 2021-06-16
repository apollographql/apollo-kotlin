plugins {
  kotlin("jvm")
}

dependencies {
  implementation(project(":apollo-api"))
  api(groovy.util.Eval.x(project, "x.dep.rx.java"))
  api(project(":deprecated-apollo-runtime"))
}


