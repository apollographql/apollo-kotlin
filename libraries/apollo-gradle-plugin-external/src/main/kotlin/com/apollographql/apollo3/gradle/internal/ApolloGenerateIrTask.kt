package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.compiler.ApolloCompiler
import com.apollographql.apollo3.compiler.IrOptions
import com.apollographql.apollo3.compiler.defaultAddTypename
import com.apollographql.apollo3.compiler.defaultAlwaysGenerateTypesMatching
import com.apollographql.apollo3.compiler.defaultDecapitalizeFields
import com.apollographql.apollo3.compiler.defaultFailOnWarnings
import com.apollographql.apollo3.compiler.defaultFieldsOnDisjointTypesMustMerge
import com.apollographql.apollo3.compiler.defaultGenerateOptionalOperationVariables
import com.apollographql.apollo3.compiler.defaultWarnOnDeprecatedUsages
import com.apollographql.apollo3.compiler.ir.toIrOperations
import com.apollographql.apollo3.compiler.ir.writeTo
import com.apollographql.apollo3.gradle.internal.ApolloGenerateSourcesFromIrTask.Companion.findCodegenSchema
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class ApolloGenerateIrTask: DefaultTask() {
  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val graphqlFiles: ConfigurableFileCollection

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val codegenSchemas: ConfigurableFileCollection

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val upstreamIrFiles: ConfigurableFileCollection

  @get:Input
  @get:Optional
  abstract val addTypename: Property<String>

  @get:Input
  @get:Optional
  abstract val fieldsOnDisjointTypesMustMerge: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val decapitalizeFields: Property<Boolean>

  @get:Input
  abstract val flattenModels: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val warnOnDeprecatedUsages: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val failOnWarnings: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val generateOptionalOperationVariables: Property<Boolean>

  @get:Input
  abstract val alwaysGenerateTypesMatching: SetProperty<String>

  @get:OutputFile
  abstract val outputFile: RegularFileProperty

  @TaskAction
  fun taskAction() {
    val irOperations = upstreamIrFiles.map { it.absolutePath to it.toIrOperations() }

    val options = IrOptions(
        executableFiles = graphqlFiles.files,
        codegenSchema = codegenSchemas.files.findCodegenSchema(),
        addTypename = addTypename.getOrElse(defaultAddTypename),
        incomingFragments = irOperations.flatMap { it.second.fragmentDefinitions },
        fieldsOnDisjointTypesMustMerge = fieldsOnDisjointTypesMustMerge.getOrElse(defaultFieldsOnDisjointTypesMustMerge),
        decapitalizeFields = decapitalizeFields.getOrElse(defaultDecapitalizeFields),
        flattenModels = flattenModels.get(),
        warnOnDeprecatedUsages = warnOnDeprecatedUsages.getOrElse(defaultWarnOnDeprecatedUsages),
        failOnWarnings = failOnWarnings.getOrElse(defaultFailOnWarnings),
        logger = logger(),
        generateOptionalOperationVariables = generateOptionalOperationVariables.getOrElse(defaultGenerateOptionalOperationVariables),
        alwaysGenerateTypesMatching = alwaysGenerateTypesMatching.get(),
    )

    ApolloCompiler.buildIrOperations(options).writeTo(outputFile.asFile.get())
  }
}