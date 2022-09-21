plugins {
  id("org.jetbrains.kotlin.jvm")
  id("org.jetbrains.kotlin.plugin.spring")
  id("application")
  id("apollo.test")
}

dependencies {
  api(golatac.lib("graphqlkotlin"))
  api(golatac.lib("kotlin.reflect").toString()) {
    because("graphqlKotlin pull kotlin-reflect and that triggers a warning like" +
        "Runtime JAR files in the classpath should have the same version.")
  }
  implementation(golatac.lib("kotlinx.coroutines.reactor").toString()) {
    because("reactor must have the same version as the coroutines version")
  }
}

application {
  mainClass.set("com.apollographql.apollo.sample.server.MainKt")
}
