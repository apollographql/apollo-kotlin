/*
 * This file is auto generated from apollo-rx2-support by rxjava3.main.kts, do not edit manually.
 */
plugins {
  id("org.jetbrains.kotlin.jvm")
}

dependencies {
  implementation(projects.apolloApi)
  api(libs.rx.java3)
  api(libs.kotlinx.coroutines.rx3)

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
