import kotlinx.validation.ExperimentalBCVApi

plugins {
  id("base")
}

buildscript {
  dependencies {
    classpath("com.apollographql.apollo:build-logic")
  }
}

apply(plugin = "org.jetbrains.kotlinx.binary-compatibility-validator")

version = property("VERSION_NAME")!!

configure<kotlinx.validation.ApiValidationExtension> {
  @OptIn(ExperimentalBCVApi::class)
  klib.enabled = true

  ignoredPackages.addAll(
      listOf(
          /**
           * In general, we rely on annotations or "internal" visibility to hide the non-public APIs. But there are a few exceptions:
           *
           * Gradle plugin: tasks and other classes must be public in order for Gradle to instantiate and decorate them.
           * SQLDelight generated sources are not generated as 'internal'.
           */
          "com.apollographql.apollo.gradle.internal",
          "com.apollographql.apollo.cache.normalized.sql.internal",
          "com.apollographql.apollo.runtime.java.internal",
          "com.apollographql.apollo.runtime.java.interceptor.internal",
          "com.apollographql.apollo.runtime.java.network.http.internal",
      )
  )
  ignoredProjects.add("apollo-testing-support-internal")
}

tasks.register("rmbuild") {
  val root = file(".")
  doLast {
    root.walk().onEnter {
      if (it.isDirectory && it.name == "build") {
        println("deleting: $it")
        it.deleteRecursively()
        false
      } else {
        true
      }
    }.count()
  }
}

apolloRoot()
