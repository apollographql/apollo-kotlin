package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.compiler.ApolloCompiler
import com.apollographql.apollo3.compiler.ExpressionAdapterInitializer
import com.apollographql.apollo3.compiler.IrOptions
import com.apollographql.apollo3.compiler.RuntimeAdapterInitializer
import com.apollographql.apollo3.compiler.ScalarInfo
import com.apollographql.apollo3.compiler.TargetLanguage
import com.apollographql.apollo3.compiler.defaultAddTypename
import com.apollographql.apollo3.compiler.defaultAlwaysGenerateTypesMatching
import com.apollographql.apollo3.compiler.defaultDecapitalizeFields
import com.apollographql.apollo3.compiler.defaultFailOnWarnings
import com.apollographql.apollo3.compiler.defaultFieldsOnDisjointTypesMustMerge
import com.apollographql.apollo3.compiler.defaultGenerateDataBuilders
import com.apollographql.apollo3.compiler.defaultGenerateOptionalOperationVariables
import com.apollographql.apollo3.compiler.defaultWarnOnDeprecatedUsages
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

@Suppress("UnstableApiUsage") // Because the gradle-api we link against has a lot of symbols still experimental
@CacheableTask
abstract class ApolloGenerateSourcesTask : ApolloGenerateSourcesBase() {
  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val graphqlFiles: ConfigurableFileCollection

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val schemaFiles: ConfigurableFileCollection

  @get:Input
  @get:Optional
  abstract val alwaysGenerateTypesMatching: SetProperty<String>

  @get:Input
  @get:Optional
  abstract val scalarTypeMapping: MapProperty<String, String>

  @get:Input
  @get:Optional
  abstract val scalarAdapterMapping: MapProperty<String, String>

  @get:Internal
  abstract val warnOnDeprecatedUsages: Property<Boolean>

  @get:Internal
  abstract val failOnWarnings: Property<Boolean>

  @get:Input
  abstract val codegenModels: Property<String>

  @get:Input
  abstract val targetLanguage: Property<TargetLanguage>

  @get:Input
  @get:Optional
  abstract val addTypename: Property<String>

  @get:Input
  abstract val flattenModels: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val generateDataBuilders: Property<Boolean>

  @get:Input
  abstract val projectPath: Property<String>

  @get:Input
  @get:Optional
  abstract val fieldsOnDisjointTypesMustMerge: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val decapitalizeFields: Property<Boolean>

  @TaskAction
  fun taskAction() {
    val codegenSchema = ApolloCompiler.buildCodegenSchema(
        schemaFiles = schemaFiles.files,
        logger = logger(),
        packageNameGenerator = packageNameGenerator,
        scalarMapping = scalarMapping(scalarTypeMapping, scalarAdapterMapping),
        codegenModels = codegenModels.get(),
        targetLanguage = targetLanguage.get(),

        generateDataBuilders = generateDataBuilders.getOrElse(defaultGenerateDataBuilders),

    )

    val irOptions = IrOptions(
        executableFiles = graphqlFiles.files,
        codegenSchema = codegenSchema,
        addTypename = addTypename.getOrElse(defaultAddTypename),
        incomingFragments = emptyList(),
        fieldsOnDisjointTypesMustMerge = fieldsOnDisjointTypesMustMerge.getOrElse(defaultFieldsOnDisjointTypesMustMerge),
        decapitalizeFields = decapitalizeFields.getOrElse(defaultDecapitalizeFields),
        flattenModels = flattenModels.get(),
        warnOnDeprecatedUsages = warnOnDeprecatedUsages.getOrElse(defaultWarnOnDeprecatedUsages),
        failOnWarnings = failOnWarnings.getOrElse(defaultFailOnWarnings),
        logger = logger(),
        generateOptionalOperationVariables = generateOptionalOperationVariables.getOrElse(defaultGenerateOptionalOperationVariables),
        alwaysGenerateTypesMatching = alwaysGenerateTypesMatching.getOrElse(defaultAlwaysGenerateTypesMatching),
    )

    val irOperations = ApolloCompiler.buildIrOperations(irOptions)

    runCodegen(codegenSchema, irOperations, ApolloCompiler.buildUsedCoordinates(irOperations), emptyList())
  }


  companion object {
    internal fun scalarMapping(scalarTypeMapping: MapProperty<String, String>, scalarAdapterMapping: MapProperty<String, String>): Map<String, ScalarInfo> {
      return scalarTypeMapping.getOrElse(emptyMap()).mapValues { (graphQLName, targetName) ->
        val adapterInitializerExpression = scalarAdapterMapping.getOrElse(emptyMap())[graphQLName]
        ScalarInfo(targetName, if (adapterInitializerExpression == null) RuntimeAdapterInitializer else ExpressionAdapterInitializer(adapterInitializerExpression))
      }
    }
  }
}
