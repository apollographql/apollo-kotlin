plugins {
  kotlin("jvm")
}

dependencies {
  api(groovy.util.Eval.x(project, "x.dep.okHttp.okHttp"))
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

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java) {
  kotlinOptions {
    allWarningsAsErrors = true
  }
}