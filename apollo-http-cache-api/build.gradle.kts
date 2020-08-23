plugins {
  `java-library`
  kotlin("jvm")
}

dependencies {
  add("compileOnly", groovy.util.Eval.x(project, "x.dep.okHttp.okHttp"))

  add("api", groovy.util.Eval.x(project, "x.dep.okio"))
}
