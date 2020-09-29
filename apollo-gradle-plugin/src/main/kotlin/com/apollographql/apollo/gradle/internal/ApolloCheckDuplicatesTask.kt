package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.compiler.ApolloMetadata
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class ApolloCheckDuplicatesTask : DefaultTask() {
  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val metadataFiles: ConfigurableFileCollection

  @get:OutputFile
  abstract val outputFile: RegularFileProperty

  @TaskAction
  fun taskAction() {
    val metadataList = metadataFiles.files.mapNotNull { ApolloMetadata.readFrom(it) }

    metadataList.flatMap { metadata ->
      metadata.fragments.map { it.fragmentName to metadata.moduleName }
    }
        .groupBy { it.first }
        .values
        .find { it.size > 1 }
        ?.run {
          throw IllegalStateException("duplicate Fragment '${get(0).first}' generated in modules: ${map { it.second }.joinToString(",")}")
        }

    metadataList.flatMap { metadata ->
      metadata.types.map { it to metadata.moduleName }
    }
        .groupBy { it.first }
        .values
        .find { it.size > 1 }
        ?.run {
          throw IllegalStateException("duplicate Type '${get(0).first}' generated in modules: ${map { it.second }.joinToString(",")}" +
              "\nUse 'alwaysGenerateTypesMatching' in a parent module to generate the type only once")
        }

    outputFile.asFile.get().run {
      parentFile.mkdirs()
      writeText("No duplicate found.")
    }
  }
}