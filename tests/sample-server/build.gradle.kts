plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.google.devtools.ksp")
  id("com.apollographql.execution")
  id("org.jetbrains.kotlin.plugin.spring")
  id("application")
}

apolloTest()

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
