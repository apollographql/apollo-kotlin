plugins {
  id("org.jetbrains.kotlin.jvm")
  id("org.jetbrains.kotlin.plugin.spring")
  id("application")
}

apolloTest()

dependencies {
  implementation(libs.graphqlkotlin)
  implementation(libs.kotlin.reflect) {
    because("graphqlKotlin pull kotlin-reflect and that triggers a warning like" +
        "Runtime JAR files in the classpath should have the same version.")
  }
  implementation(libs.kotlinx.coroutines.reactor) {
    because("reactor must have the same version as the coroutines version")
  }
  compileOnly(libs.apollo.annotations) {
    because("""
      We unconditionally opt-in ApolloExperimental in all the tests and we need the symbol in the 
      classpath to prevent a warning
    """.trimIndent())
  }
}

application {
  mainClass.set("com.apollographql.apollo.sample.server.MainKt")
}
