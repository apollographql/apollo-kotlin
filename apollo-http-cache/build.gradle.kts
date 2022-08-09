plugins {
  id("org.jetbrains.kotlin.jvm")
}

dependencies {
  api(libs.okHttp)
  api(projects.apolloApi)
  api(projects.apolloRuntime)
  implementation(libs.moshi)
  implementation(libs.kotlinx.datetime)

  testImplementation(projects.apolloMockserver)
  testImplementation(libs.kotlin.test.junit)
  testImplementation(libs.truth)
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
