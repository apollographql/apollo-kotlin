package com.apollographql.apollo3.gradle.api

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention

val Project.javaConventionOrThrow
  get() = convention.getPlugin(JavaPluginConvention::class.java)

val Project.javaConvention
  get() = try {
    javaConventionOrThrow
  } catch (e: Exception) {
    null
  }

