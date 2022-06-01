import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  antlr
  kotlin("jvm")
  id("com.google.devtools.ksp")
}

dependencies {
  antlr(groovy.util.Eval.x(project, "x.dep.antlr.antlr"))
  implementation(groovy.util.Eval.x(project, "x.dep.antlr.runtime"))
  api(okio())
  api(projects.apolloAnnotations)

  implementation(groovy.util.Eval.x(project, "x.dep.moshi.moshi"))
  implementation(groovy.util.Eval.x(project, "x.dep.moshi.sealedRuntime"))

  ksp(groovy.util.Eval.x(project, "x.dep.moshi.sealedCodegen"))
  ksp(groovy.util.Eval.x(project, "x.dep.moshi.ksp"))

  testImplementation(kotlin("test-junit"))
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
  kotlinOptions {
    allWarningsAsErrors = true
  }
}

val jar by tasks.getting(Jar::class) {
  manifest {
    attributes("Automatic-Module-Name" to "com.apollographql.apollo3.ast")
  }
}