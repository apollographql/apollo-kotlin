import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.google.devtools.ksp")
  id("com.apollographql.execution")
  id("org.jetbrains.kotlin.plugin.spring")
  id("application")
}

apolloTest(
    // Can't use apiVersion KOTLIN_2_0 when using languageVersion KOTLIN_1_9, which is the case here because we're using KSP 1
    // TODO: move to KSP 2 and remove this when https://github.com/google/ksp/issues/1823 is resolved
    kotlinCompilerOptions = KotlinCompilerOptions(
        version = KotlinVersion.KOTLIN_1_9,
    )
)

dependencies {
  implementation(libs.apollo.annotations)
  implementation(libs.kotlinx.coroutines)
  implementation(libs.atomicfu.library)
  implementation(libs.apollo.execution)

  implementation(platform(libs.http4k.bom.get()))
  implementation(libs.http4k.core)
  implementation(libs.http4k.server.jetty)
  implementation(libs.slf4j.nop.get().toString()) {
    because("jetty uses SL4F")
  }
}

apolloExecution {
  service("sampleserver") {
    packageName = "sample.server"
  }
}

application {
  mainClass.set("com.apollographql.apollo.sample.server.MainKt")
}

