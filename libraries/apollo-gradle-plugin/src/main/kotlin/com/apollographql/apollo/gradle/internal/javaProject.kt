package com.apollographql.apollo.gradle.internal

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension

internal val Project.javaExtensionOrThrow: JavaPluginExtension
  get() = javaExtension ?: throw IllegalStateException("Apollo: no 'java' extension found.")

internal val Project.javaExtension
  get() = extensions.getByName("java") as? JavaPluginExtension

