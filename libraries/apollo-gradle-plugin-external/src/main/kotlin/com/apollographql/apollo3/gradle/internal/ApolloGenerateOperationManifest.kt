package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.compiler.toIrOperations
import com.apollographql.apollo3.compiler.writeManifestTo
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class ApolloGenerateOperationManifest: DefaultTask() {
  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val irOperations: RegularFileProperty

  @get:Input
  abstract val operationManifestFormat: Property<String>

  @get:OutputFile
  abstract val operationManifestFile: RegularFileProperty

  @TaskAction
  fun taskAction() {
    irOperations.get().asFile.toIrOperations().writeManifestTo(operationManifestFile.get().asFile, operationManifestFormat.get())
  }
}