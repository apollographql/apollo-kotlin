plugins {
  `java-library`
  kotlin("jvm")
}

dependencies {
  add("implementation", project(":apollo-api"))
  add("implementation", groovy.util.Eval.x(project, "x.dep.kotlin.stdLib"))
  add("api", groovy.util.Eval.x(project, "x.dep.rx.java"))
  add("compileOnly", project(":apollo-runtime"))
}

tasks.withType<Javadoc> {
  options.encoding = "UTF-8"
}

