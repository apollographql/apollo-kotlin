plugins {
  kotlin("jvm")
}

dependencies {
  implementation(projects.apolloApi)
  api(groovy.util.Eval.x(project, "x.dep.rx2"))
  api(groovy.util.Eval.x(project, "x.dep.kotlin.coroutinesRx2"))

  api(projects.apolloRuntime)
  api(projects.apolloNormalizedCache)
}

val jar by tasks.getting(Jar::class) {
  manifest {
    attributes("Automatic-Module-Name" to "com.apollographql.apollo3.rx2")
  }
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java) {
  kotlinOptions {
    allWarningsAsErrors = true
  }
}
