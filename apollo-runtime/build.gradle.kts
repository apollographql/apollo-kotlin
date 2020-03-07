plugins {
  `java-library`
}

java {
  targetCompatibility = JavaVersion.VERSION_1_8
  sourceCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
  add("api", project(":apollo-api")) // apollo-espresso-support uses some apollo-api internals
  add("api", project(":apollo-http-cache-api"))
  add("api", groovy.util.Eval.x(project, "x.dep.okHttp.okHttp"))
  add("compileOnly", groovy.util.Eval.x(project, "x.dep.jetbrainsAnnotations"))
  add("implementation", groovy.util.Eval.x(project, "x.dep.cache"))

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
