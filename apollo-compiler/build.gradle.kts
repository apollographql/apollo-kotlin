import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  antlr
  `java-library`
  kotlin("jvm")
  kotlin("kapt")
}

dependencies {
  add("antlr", groovy.util.Eval.x(project, "x.dep.antlr.antlr"))
  add("implementation", groovy.util.Eval.x(project, "x.dep.kotlin.stdLib"))
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

tasks.register("pluginVersion") {
  val outputDir = file("src/generated/kotlin")

  inputs.property("version", version)
  outputs.dir(outputDir)

  doLast {
    val versionFile = file("$outputDir/com/apollographql/android/Version.kt")
    versionFile.parentFile.mkdirs()
    versionFile.writeText("""// Generated file. Do not edit!
package com.apollographql.android
val VERSION = "${project.version}"
""")
  }
}

tasks.getByName("compileKotlin").dependsOn("pluginVersion")

tasks.withType<Checkstyle> {
  exclude("**com/apollographql/apollo/compiler/parser/antlr/**")
}

// since test/graphql is not an input to Test tasks, they're not run with the changes made in there.
tasks.withType<Test>().configureEach { outputs.upToDateWhen { false } }
