plugins {
  kotlin("jvm")
}

dependencies {
  implementation(projects.apolloApi)
  api(libs.rx.java2)
  api(libs.kotlinx.coroutines.rx2)

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
