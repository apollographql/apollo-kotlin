import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  antlr
  `java-library`
  kotlin("jvm")
  kotlin("kapt")
}

dependencies {
  add("antlr", groovy.util.Eval.x(project, "x.dep.antlr.antlr"))
  add("implementation", groovy.util.Eval.x(project, "x.dep.moshi.adapters"))
  add("implementation", groovy.util.Eval.x(project, "x.dep.moshi.moshi"))
  add("implementation", groovy.util.Eval.x(project, "x.dep.poet.java"))
  add("implementation", groovy.util.Eval.x(project, "x.dep.poet.kotlin"))
  add("implementation", project(":apollo-api"))

  add("kapt", groovy.util.Eval.x(project, "x.dep.moshi.kotlinCodegen"))


  add("testImplementation", groovy.util.Eval.x(project, "x.dep.compiletesting"))
  add("testImplementation", groovy.util.Eval.x(project, "x.dep.kotlinCompileTesting"))
  add("testImplementation", groovy.util.Eval.x(project, "x.dep.junit"))
  add("testImplementation", groovy.util.Eval.x(project, "x.dep.truth"))
}

abstract class GeneratePluginVersion : DefaultTask() {
  @get:org.gradle.api.tasks.Input
  abstract val version: Property<String>

  @get:org.gradle.api.tasks.OutputFile
  abstract val outputFile: RegularFileProperty

  @org.gradle.api.tasks.TaskAction
  fun taskAction() {
    val versionFile = outputFile.asFile.get()
    versionFile.parentFile.mkdirs()
    versionFile.writeText("""// Generated file. Do not edit!
package com.apollographql.apollo.compiler
val VERSION = "${project.version}"
""")
  }
}

val pluginVersionTaskProvider = tasks.register("pluginVersion", GeneratePluginVersion::class.java) {
  outputFile.set(project.layout.buildDirectory.file("generated/kotlin/com/apollographql/apollo/compiler/Version.kt"))
  version.set(project.version.toString())
}

tasks.withType(KotlinCompile::class.java) {
  val versionFileProvider = pluginVersionTaskProvider.flatMap { it.outputFile }
  source(versionFileProvider)
}

tasks.withType<Checkstyle> {
  exclude("**com/apollographql/apollo/compiler/parser/antlr/**")
}

// since test/graphql is not an input to Test tasks, they're not run with the changes made in there.
tasks.withType<Test>().configureEach {
  inputs.dir("src/test/graphql")
}
