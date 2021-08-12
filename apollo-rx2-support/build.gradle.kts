plugins {
  kotlin("jvm")
}

dependencies {
  implementation(project(":apollo-api"))
  api(groovy.util.Eval.x(project, "x.dep.rx2"))
  api(groovy.util.Eval.x(project, "x.dep.kotlin.coroutinesRx2"))

  api(project(":apollo-runtime"))
  api(project(":apollo-normalized-cache"))
}


