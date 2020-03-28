plugins {
  `java-library`
}

dependencies {
  add("api", project(":apollo-api"))
  add("api", project(":apollo-cache"))
  add("api", project(":apollo-http-cache-api"))
  add("api", groovy.util.Eval.x(project, "x.dep.okHttp.okHttp"))
  add("compileOnly", groovy.util.Eval.x(project, "x.dep.jetbrainsAnnotations"))

  add("testCompileOnly", groovy.util.Eval.x(project, "x.dep.jetbrainsAnnotations"))
  add("testCompile", groovy.util.Eval.x(project, "x.dep.mockito"))
  add("testImplementation", groovy.util.Eval.x(project, "x.dep.junit"))
  add("testImplementation", groovy.util.Eval.x(project, "x.dep.truth"))
  add("testImplementation", groovy.util.Eval.x(project, "x.dep.okHttp.mockWebServer"))
  add("testImplementation", project(":apollo-rx2-support"))
}

tasks.withType<Javadoc> {
  options.encoding = "UTF-8"
}
