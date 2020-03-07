import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  antlr
  `java-library`
  kotlin("jvm")
  kotlin("kapt")
}

java {
  targetCompatibility = JavaVersion.VERSION_1_8
  sourceCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
  add("antlr", groovy.util.Eval.x(project, "x.dep.antlr.antlr"))
  add("implementation", groovy.util.Eval.x(project, "x.dep.kotlin.stdLib"))
  add("implementation", groovy.util.Eval.x(project, "x.dep.moshi.adapters"))
  add("implementation", groovy.util.Eval.x(project, "x.dep.moshi.kotlin"))
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

tasks.withType<KotlinCompile> {
  dependsOn("generateGrammarSource")
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
