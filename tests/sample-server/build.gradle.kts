plugins {
  id("org.jetbrains.kotlin.jvm")
  id("org.jetbrains.kotlin.plugin.spring")
  id("application")
  id("apollo.test")
}

dependencies {
  implementation(golatac.lib("graphqlkotlin"))
  implementation(golatac.lib("kotlin.reflect")) {
    because("graphqlKotlin pull kotlin-reflect and that triggers a warning like" +
        "Runtime JAR files in the classpath should have the same version.")
  }
  implementation(golatac.lib("kotlinx.coroutines.reactor")) {
    because("reactor must have the same version as the coroutines version")
  }
  compileOnly(golatac.lib("apollo.annotations")) {
    because("""
      We unconditionally opt-in ApolloExperimental in all the tests and we need the symbol in the 
      classpath to prevent a warning
    """.trimIndent())
  }
}

application {
  mainClass.set("com.apollographql.apollo.sample.server.MainKt")
}
