plugins {
  `java-library`
  kotlin("jvm")
}

dependencies {
  api(project(":apollo-api"))
  implementation(groovy.util.Eval.x(project, "x.dep.kotlin.stdLib"))

  testImplementation(groovy.util.Eval.x(project, "x.dep.junit"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.truth"))
}

tasks.withType<Javadoc> {
  options.encoding = "UTF-8"
}

