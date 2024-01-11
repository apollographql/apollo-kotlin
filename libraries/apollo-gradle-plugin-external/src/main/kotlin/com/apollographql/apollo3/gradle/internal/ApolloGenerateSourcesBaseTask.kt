package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.compiler.OperationOutputGenerator
import com.apollographql.apollo3.compiler.PackageNameGenerator
import com.apollographql.apollo3.compiler.hooks.ApolloCompilerJavaHooks
import com.apollographql.apollo3.compiler.hooks.ApolloCompilerKotlinHooks
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

abstract class ApolloGenerateSourcesBaseTask : DefaultTask() {
  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val graphqlFiles: ConfigurableFileCollection

  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val codegenOptionsFile: RegularFileProperty

  @get:Input
  @get:Optional
  abstract val packageName: Property<String>

  @get:Input
  @get:Optional
  abstract val packageNamesFromFilePaths: Property<Boolean>

  @Internal
  var packageNameRoots: Set<String>? = null

  @Internal
  var packageNameGenerator: PackageNameGenerator? = null

  @Input
  fun getPackageNameGeneratorVersion() = packageNameGenerator?.version ?: ""

  @get:Internal
  var operationOutputGenerator: OperationOutputGenerator? = null

  @Input
  fun getOperationOutputGeneratorVersion() = operationOutputGenerator?.version ?: ""

  @get:Internal
  var compilerKotlinHooks: ApolloCompilerKotlinHooks? = null

  @Input
  fun getCompilerKotlinHooksVersion() = compilerKotlinHooks?.version ?: ""

  @get:Internal
  var compilerJavaHooks: ApolloCompilerJavaHooks? = null

  @Input
  fun getCompilerJavaHooksVersion() = compilerJavaHooks?.version ?: ""

  @get:OutputFile
  @get:Optional
  abstract val operationManifestFile: RegularFileProperty

  @get:OutputDirectory
  abstract val outputDir: DirectoryProperty
}

