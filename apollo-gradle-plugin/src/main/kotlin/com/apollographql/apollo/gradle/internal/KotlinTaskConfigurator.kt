package com.apollographql.apollo.gradle.internal

import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

object KotlinTaskConfigurator {
  fun registerGeneratedDirectory(kotlinProjectExtension: KotlinProjectExtension, codegenProvider: TaskProvider<ApolloGenerateSourcesTask>) {
    kotlinProjectExtension.sourceSets.getByName("main").kotlin.srcDir(codegenProvider.flatMap { it.outputDir })
  }
}