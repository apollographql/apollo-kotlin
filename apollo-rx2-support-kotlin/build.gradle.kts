plugins {
  kotlin("jvm")
}

dependencies {
  implementation(project(":apollo-api"))
  api(groovy.util.Eval.x(project, "x.dep.rx.java"))
  api(project(":apollo-runtime-kotlin"))
  api(project(":apollo-normalized-cache"))
}


