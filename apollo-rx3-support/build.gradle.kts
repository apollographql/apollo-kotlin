/*
 * This file is auto generated from apollo-rx2-support by rxjava3.main.kts, do not edit manually.
 */
plugins {
  kotlin("jvm")
}

dependencies {
  implementation(project(":apollo-api"))
  api(groovy.util.Eval.x(project, "x.dep.rx3"))
  api(groovy.util.Eval.x(project, "x.dep.kotlin.coroutinesRx3"))

  api(project(":apollo-runtime"))
  api(project(":apollo-normalized-cache"))
}


