import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  antlr
  id("org.jetbrains.kotlin.jvm")
  id("apollo.library")
  id("com.google.devtools.ksp")
}

apolloLibrary {
  javaModuleName("com.apollographql.apollo3.ast")
}

dependencies {
  antlr(golatac.lib("antlr"))
  implementation(golatac.lib("antlr.runtime"))
  api(okio())
  api(project(":libraries:apollo-annotations"))

  implementation(golatac.lib("moshi"))
  implementation(golatac.lib("moshix.sealed.runtime"))

  ksp(golatac.lib("moshix.sealed.codegen"))
  ksp(golatac.lib("moshix.ksp"))

  testImplementation(golatac.lib("kotlin.test.junit"))
}

// Only expose the antlr runtime dependency
// See https://github.com/gradle/gradle/issues/820#issuecomment-288838412
configurations[JavaPlugin.API_CONFIGURATION_NAME].let { apiConfiguration ->
  apiConfiguration.setExtendsFrom(apiConfiguration.extendsFrom.filter { it.name != "antlr" })
}

tasks.withType(KotlinCompile::class.java) {
  // This used to work and fails now. Strangely enough, it fails on both `dev-3.x` and `main` as of writing while both these branches have
  // compiled successfully before...
  dependsOn("generateGrammarSource")
}
