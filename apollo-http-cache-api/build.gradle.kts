plugins {
  kotlin("jvm")
}

dependencies {
  compileOnly(groovy.util.Eval.x(project, "x.dep.okHttp.okHttp"))

  api(groovy.util.Eval.x(project, "x.dep.okio"))
}
