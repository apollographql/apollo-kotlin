package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.compiler.toUsedCoordinates
import com.apollographql.apollo3.compiler.writeTo
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class ApolloGenerateUsedCoordinatesAggregateTask : DefaultTask() {
  @get:OutputFile
  @get:Optional
  abstract val outputFile: RegularFileProperty

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val incomingUsedCoordinates: ConfigurableFileCollection

  @TaskAction
  fun taskAction() {
    val coordinates = incomingUsedCoordinates.files.fold(setOf<String>()) { acc, n -> acc + n.toUsedCoordinates() }

    // Merge all files into a single list
    coordinates.writeTo(outputFile.get().asFile)
  }
}
