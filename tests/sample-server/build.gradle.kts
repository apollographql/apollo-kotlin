import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.google.devtools.ksp")
  id("com.apollographql.apollo3")
  id("org.jetbrains.kotlin.plugin.spring")
  id("application")
}

apolloTest()

dependencies {
  implementation(libs.apollo.execution)
  implementation(libs.apollo.api)
  implementation(libs.kotlinx.coroutines)
  implementation(libs.atomicfu.library)

  implementation(platform(libs.http4k.bom.get()))
  implementation(libs.http4k.core)
  implementation(libs.http4k.server.jetty)
  implementation(libs.slf4j.get().toString()) {
    because("jetty uses SL4F")
  }

  ksp(apollo.apolloKspProcessor(file("src/main/resources/schema.graphqls"), "sampleserver", "sample.server"))
}

application {
  mainClass.set("com.apollographql.apollo.sample.server.MainKt")
}

// Can't use apiVersion KOTLIN_2_0 when using languageVersion KOTLIN_1_9, which is the case here because we're using KSP 1
// TODO: move to KSP 2 and remove this when https://github.com/google/ksp/issues/1823 is resolved
tasks.withType(KotlinCompilationTask::class.java).configureEach {
  compilerOptions {
    apiVersion.set(KotlinVersion.KOTLIN_1_9)
  }
}
