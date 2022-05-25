plugins {
  id("com.apollographql.apollo3")
}

buildscript {
  apply(from = "../../../gradle/dependencies.gradle")

  repositories {
    maven {
      url = uri("../../../build/localMaven")
    }
    gradlePluginPortal()
  }

  dependencies {
    classpath(groovy.util.Eval.x(project, "x.dep.kotlinPlugin"))
  }
}

apply(plugin = "org.jetbrains.kotlin.jvm")

