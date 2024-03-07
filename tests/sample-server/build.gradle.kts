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
