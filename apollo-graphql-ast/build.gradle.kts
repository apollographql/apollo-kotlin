import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  antlr
  kotlin("jvm")
}

dependencies {
  antlr(groovy.util.Eval.x(project, "x.dep.antlr.antlr"))
  implementation(groovy.util.Eval.x(project, "x.dep.antlr.runtime"))
  api(groovy.util.Eval.x(project, "x.dep.okio"))

  testImplementation(kotlin("test-junit"))
}

// Only expose the antlr runtime dependency
// See https://github.com/gradle/gradle/issues/820#issuecomment-288838412
configurations[JavaPlugin.API_CONFIGURATION_NAME].let { apiConfiguration ->
  apiConfiguration.setExtendsFrom(apiConfiguration.extendsFrom.filter { it.name != "antlr" })
}

tasks.withType(KotlinCompile::class.java) {
  kotlinOptions {
    // Gradle forces 1.3.72 for the time being so compile against 1.3 stdlib for the time being
    // See https://issuetracker.google.com/issues/166582569
    apiVersion = "1.3"
  }
  // This used to work and fails now. Strangely enough, it fails on both `dev-3.x` and `main` as of writing while both these branches have
  // compiled successfully before...
  dependsOn("generateGrammarSource")
}
