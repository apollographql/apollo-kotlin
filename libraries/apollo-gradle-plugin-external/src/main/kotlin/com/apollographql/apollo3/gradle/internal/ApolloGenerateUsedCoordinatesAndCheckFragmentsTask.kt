package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.compiler.ApolloCompiler
import com.apollographql.apollo3.compiler.PackageNameGenerator
import com.apollographql.apollo3.compiler.TargetLanguage
import com.apollographql.apollo3.compiler.defaultGenerateDataBuilders
import com.apollographql.apollo3.compiler.ir.toIrOperations
import com.apollographql.apollo3.compiler.toUsedCoordinates
import com.apollographql.apollo3.compiler.writeTo
import com.apollographql.apollo3.gradle.internal.ApolloGenerateSourcesTask.Companion.scalarMapping
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class ApolloGenerateUsedCoordinatesAndCheckFragmentsTask : DefaultTask() {
  @get:OutputFile
  @get:Optional
  abstract val outputFile: RegularFileProperty

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val downStreamIrOperations: ConfigurableFileCollection

  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val irOperations: RegularFileProperty


  @TaskAction
  fun taskAction() {
    val usedCoordinates = ApolloCompiler.buildUsedCoordinates((downStreamIrOperations.files + irOperations.get().asFile).toList())

    usedCoordinates.writeTo(outputFile.get().asFile)
  }
}
