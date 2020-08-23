plugins {
  `java-library`
  kotlin("jvm")
}

dependencies {
  api(project(":apollo-runtime"))
  api(project(":apollo-api"))
  api(groovy.util.Eval.x(project, "x.dep.kotlin.coroutines"))

  testImplementation(groovy.util.Eval.x(project, "x.dep.junit"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.truth"))
}

tasks.withType<Javadoc> {
  options.encoding = "UTF-8"
}
