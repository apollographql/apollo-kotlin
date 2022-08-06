plugins {
  id("org.jetbrains.kotlin.jvm")
  id("org.jetbrains.kotlin.plugin.spring")
  id("application")
}

dependencies {
  api(libs.graphqlKotlin)
  api(libs.kotlin.reflect.get().toString()) {
    because("graphqlKotlin pull kotlin-reflect and that triggers a warning like" +
        "Runtime JAR files in the classpath should have the same version.")
  }
  implementation(libs.kotlinx.coroutines.reactor.get().toString()) {
    because("reactor must have the same version as the coroutines version")
  }
}

application {
  mainClass.set("com.apollographql.apollo.sample.server.MainKt")
}
