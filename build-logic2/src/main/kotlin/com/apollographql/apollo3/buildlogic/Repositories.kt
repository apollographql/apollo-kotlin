package com.apollographql.apollo3.buildlogic

import org.gradle.api.Project

fun Project.configureRepositories() {
  repositories.apply {
    mavenCentral()
    google()
    jcenter {
      content {
        // https://github.com/Kotlin/kotlinx-nodejs/issues/16
        includeModule("org.jetbrains.kotlinx", "kotlinx-nodejs")
      }
    }
  }
}
