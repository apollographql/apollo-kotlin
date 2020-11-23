plugins {
  `java-library`
  kotlin("jvm")
}

metalava {
  hiddenPackages += setOf("com.apollographql.apollo.internal")
}

dependencies {
  api(project(":apollo-api"))
  api(project(":apollo-normalized-cache"))
  api(project(":apollo-http-cache-api"))
  api(groovy.util.Eval.x(project, "x.dep.okHttp.okHttp"))

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
