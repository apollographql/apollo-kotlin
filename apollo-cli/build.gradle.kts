import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("application")
}

dependencies {
  implementation(projects.apolloTooling)
  implementation(projects.apolloAnnotations)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.clikt)
}

tasks.withType(KotlinCompile::class.java) {
  kotlinOptions {
    allWarningsAsErrors = true
  }
}

application {
  mainClass.set("com.apollographql.apollo3.cli.MainKt")
}
