plugins {
  kotlin("jvm")
}

dependencies {
  api(groovy.util.Eval.x(project, "x.dep.okHttp.okHttp"))
  api(groovy.util.Eval.x(project, "x.dep.okio"))
  api(projects.apolloApi)
  api(projects.apolloRuntime)
  implementation(groovy.util.Eval.x(project, "x.dep.moshi.moshi"))
  implementation(groovy.util.Eval.x(project, "x.dep.kotlinxdatetime"))

  testImplementation(projects.apolloMockserver)
  testImplementation(kotlin("test-junit"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.truth"))
}

val jar by tasks.getting(Jar::class) {
  manifest {
    attributes("Automatic-Module-Name" to "com.apollographql.apollo3.cache.http")
  }
}
