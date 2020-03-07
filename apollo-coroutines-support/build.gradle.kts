plugins {
  `java-library`
  kotlin("jvm")
}

java {
  targetCompatibility = JavaVersion.VERSION_1_8
  sourceCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
  add("compileOnly", project(":apollo-runtime"))
  add("compileOnly", project(":apollo-api"))
  add("implementation", groovy.util.Eval.x(project, "x.dep.kotlin.coroutines.core"))
  add("implementation", groovy.util.Eval.x(project, "x.dep.kotlin.stdLib"))

  add("testImplementation", groovy.util.Eval.x(project, "x.dep.junit"))
  add("testImplementation", groovy.util.Eval.x(project, "x.dep.truth"))
  add("testImplementation", groovy.util.Eval.x(project, "x.dep.okHttp.mockWebServer"))
}

tasks.withType<Javadoc> {
  options.encoding = "UTF-8"
}
