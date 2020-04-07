plugins {
  `java-library`
}

dependencies {
  compileOnly(groovy.util.Eval.x(project, "x.dep.jetbrainsAnnotations"))

  api(project(":apollo-api"))
  implementation(groovy.util.Eval.x(project, "x.dep.cache"))

  testImplementation(groovy.util.Eval.x(project, "x.dep.junit"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.truth"))
}

tasks.withType<Javadoc> {
  options.encoding = "UTF-8"
}

