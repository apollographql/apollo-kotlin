import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm")
  id("com.google.devtools.ksp")
}

dependencies {
  implementation(projects.apolloAst)
  implementation(projects.apolloNormalizedCacheApi) {
    because("To generate the CacheResolver")
  }
  implementation(groovy.util.Eval.x(project, "x.dep.poet.kotlin").toString()) {
    // We don't use any of the KotlinPoet kotlin-reflect features
    exclude(module = "kotlin-reflect")
  }
  implementation(groovy.util.Eval.x(project, "x.dep.poet.java"))

  implementation(groovy.util.Eval.x(project, "x.dep.moshi.adapters"))
  implementation(groovy.util.Eval.x(project, "x.dep.moshi.moshi"))
  implementation(groovy.util.Eval.x(project, "x.dep.moshi.sealedRuntime"))

  ksp(groovy.util.Eval.x(project, "x.dep.moshi.sealedCodegen"))
  ksp(groovy.util.Eval.x(project, "x.dep.moshi.ksp"))

  testImplementation(groovy.util.Eval.x(project, "x.dep.kotlinCompileTesting"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.javaCompileTesting"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.truth"))
  testImplementation(kotlin("test-junit"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.testParameterInjector"))
}

abstract class GeneratePluginVersion : DefaultTask() {
  @get:org.gradle.api.tasks.Input
  abstract val version: Property<String>

  @get:org.gradle.api.tasks.OutputDirectory
  abstract val outputDir: DirectoryProperty

  @org.gradle.api.tasks.TaskAction
  fun taskAction() {
    val versionFile = File(outputDir.asFile.get(), "com/apollographql/apollo3/compiler/Version.kt")
    versionFile.parentFile.mkdirs()
    versionFile.writeText("""// Generated file. Do not edit!
package com.apollographql.apollo3.compiler
const val APOLLO_VERSION = "${project.version}"
""")
  }
}

val pluginVersionTaskProvider = tasks.register("pluginVersion", GeneratePluginVersion::class.java) {
  outputDir.set(project.layout.buildDirectory.dir("generated/kotlin/"))
  version.set(project.version.toString())
}

configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
  val versionFileProvider = pluginVersionTaskProvider.flatMap { it.outputDir }
  sourceSets.getByName("main").kotlin.srcDir(versionFileProvider)
}

tasks.withType(KotlinCompile::class.java) {
  // Fixes the warning below:
  // "Task ':apollo-android:apollo-compiler:kaptGenerateStubsKotlin' uses the output of task ':apollo-android:apollo-compiler:pluginVersion', without declaring an explicit dependency"
  dependsOn(pluginVersionTaskProvider)
}

tasks.withType(KotlinCompile::class.java) {
  kotlinOptions {
    allWarningsAsErrors = true
  }
}

// since test/graphql is not an input to Test tasks, they're not run with the changes made in there.
tasks.withType<Test>().configureEach {
  inputs.dir("src/test/graphql")
  inputs.dir("src/test/sdl")
  inputs.dir("src/test/typename")
  inputs.dir("src/test/usedtypes")
  inputs.dir("src/test/validation")
}

val jar by tasks.getting(Jar::class) {
  manifest {
    attributes("Automatic-Module-Name" to "com.apollographql.apollo3.compiler")
  }
}
