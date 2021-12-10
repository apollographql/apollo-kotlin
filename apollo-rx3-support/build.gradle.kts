/*
 * This file is auto generated from apollo-rx2-support by rxjava3.main.kts, do not edit manually.
 */
plugins {
  kotlin("jvm")
}

dependencies {
  implementation(projects.apolloApi)
  api(groovy.util.Eval.x(project, "x.dep.rx3"))
  api(groovy.util.Eval.x(project, "x.dep.kotlin.coroutinesRx3"))

  api(projects.apolloRuntime)
  api(projects.apolloNormalizedCache)
}

val jar by tasks.getting(Jar::class) {
  manifest {
    attributes("Automatic-Module-Name" to "com.apollographql.apollo3.rx3")
  }
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java) {
  kotlinOptions {
    allWarningsAsErrors = true
  }
}
