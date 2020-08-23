plugins {
  `java-library`
  kotlin("jvm")
}

dependencies {
  add("implementation", project(":apollo-api"))
  add("api", groovy.util.Eval.x(project, "x.dep.rx.java"))
  add("api", project(":apollo-runtime"))
}

tasks.withType<Javadoc> {
  options.encoding = "UTF-8"
}

