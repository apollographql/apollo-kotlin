plugins {
  antlr
  id("org.jetbrains.kotlin.jvm")
  id("apollo.library")
  id("org.jetbrains.kotlin.plugin.serialization")
}

apolloLibrary {
  javaModuleName("com.apollographql.apollo3.ast")
}

dependencies {
  antlr(golatac.lib("antlr"))
  implementation(golatac.lib("antlr.runtime"))
  api(okio())
  api(project(":apollo-annotations"))

  implementation(golatac.lib("kotlinx.serialization.json"))

  testImplementation(golatac.lib("kotlin.test.junit"))
}

// Only expose the antlr runtime dependency
// See https://github.com/gradle/gradle/issues/820#issuecomment-288838412
configurations[JavaPlugin.API_CONFIGURATION_NAME].let { apiConfiguration ->
  apiConfiguration.setExtendsFrom(apiConfiguration.extendsFrom.filter { it.name != "antlr" })
}

// See https://github.com/gradle/gradle/issues/19555
tasks.named("compileKotlin") {
  dependsOn("generateGrammarSource")
}
tasks.named("compileTestKotlin") {
  dependsOn("generateTestGrammarSource")
}
