package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.compiler.ApolloCompiler
import com.apollographql.apollo3.compiler.toCodegenSchemaOptions
import com.apollographql.apollo3.compiler.writeTo
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
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
abstract class ApolloGenerateCodegenSchemaTask : DefaultTask() {
  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val schemaFiles: ConfigurableFileCollection

  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val codegenSchemaOptionsFile: RegularFileProperty

  @get:Input
  @get:Optional
  abstract val packageName: Property<String>

  @get:Input
  @get:Optional
  abstract val rootPackageName: Property<String>

  @Internal
  var packageNameRoots: Set<String>? = null

  @get:OutputFile
  @get:Optional
  abstract val codegenSchemaFile: RegularFileProperty

  @TaskAction
  fun taskAction() {
    val packageNameGenerator = packageNameGenerator(packageName, rootPackageName, packageNameRoots!!)

    ApolloCompiler.buildCodegenSchema(
        schemaFiles = schemaFiles.files,
        packageNameGenerator = packageNameGenerator,
        logger = logger(),
        codegenSchemaOptions = codegenSchemaOptionsFile.get().asFile.toCodegenSchemaOptions(),
    ).writeTo(codegenSchemaFile.get().asFile)
  }
}
