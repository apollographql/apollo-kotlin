package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.compiler.ApolloMetadata
import com.apollographql.apollo3.compiler.codegen.ResolverKey
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class ApolloCheckDuplicatesTask : DefaultTask() {
  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val metadataFiles: ConfigurableFileCollection

  @get:OutputFile
  abstract val outputFile: RegularFileProperty

  @TaskAction
  fun taskAction() {
    val metadataList = metadataFiles.files.mapNotNull {
      ApolloMetadata.readFrom(it)
    }

    val duplicates = metadataList.flatMap { metadata ->
      metadata.compilerMetadata.resolverInfo.entries.map { it.key to metadata.moduleName }
    }
        .groupBy { it.first }
        .values
        .filter { it.size > 1 }

    if (duplicates.isNotEmpty()) {
      val duplicateMessage = duplicates.joinToString(separator = "\n") { list: List<Pair<ResolverKey, String>> ->
        "duplicate Type '${list.first().first}' generated in modules: ${list.joinToString(",") { it.second }}"
      }
      val recommendationMessage = "Use 'alwaysGenerateTypesMatching' in a parent module to generate the type only once"
      error("$duplicateMessage\n$recommendationMessage")
    }


    outputFile.asFile.get().run {
      parentFile.mkdirs()
      writeText("No duplicate found.")
    }
  }
}