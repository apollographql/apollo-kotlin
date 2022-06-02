import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm")
  id("application")
}

dependencies {
  implementation(projects.apolloTooling)
  implementation(projects.apolloAnnotations)
  implementation(groovy.util.Eval.x(project, "x.dep.kotlinxserializationjson"))
  implementation(groovy.util.Eval.x(project, "x.dep.clikt"))
}

tasks.withType(KotlinCompile::class.java) {
  kotlinOptions {
    allWarningsAsErrors = true
  }
}

application {
  mainClass.set("com.apollographql.apollo3.cli.MainKt")
}