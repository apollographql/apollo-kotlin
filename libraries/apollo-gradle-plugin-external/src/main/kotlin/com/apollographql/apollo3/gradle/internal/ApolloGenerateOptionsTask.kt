package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.compiler.CodegenOptions
import com.apollographql.apollo3.compiler.CodegenSchemaOptions
import com.apollographql.apollo3.compiler.ExpressionAdapterInitializer
import com.apollographql.apollo3.compiler.GeneratedMethod
import com.apollographql.apollo3.compiler.IrOptions
import com.apollographql.apollo3.compiler.JavaNullable
import com.apollographql.apollo3.compiler.RuntimeAdapterInitializer
import com.apollographql.apollo3.compiler.ScalarInfo
import com.apollographql.apollo3.compiler.TargetLanguage
import com.apollographql.apollo3.compiler.writeTo
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class ApolloGenerateOptionsTask : DefaultTask() {
  /**
   * CodegenSchemaOptions
   */
  @get:Input
  abstract val targetLanguage: Property<TargetLanguage>

  @get:Input
  @get:Optional
  abstract val scalarTypeMapping: MapProperty<String, String>

  @get:Input
  @get:Optional
  abstract val scalarAdapterMapping: MapProperty<String, String>

  @get:Input
  @get:Optional
  abstract val codegenModels: Property<String>

  @get:Input
  @get:Optional
  abstract val generateDataBuilders: Property<Boolean>

  @get:OutputFile
  abstract val codegenSchemaOptionsFile: RegularFileProperty

  /**
   * IrOptions
   */
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
  @get:Optional
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
  @get:Optional
  abstract val alwaysGenerateTypesMatching: SetProperty<String>

  @get:OutputFile
  abstract val irOptionsFile: RegularFileProperty

  /**
   * CommonCodegenOptions
   */
  @get:Input
  @get:Optional
  abstract val useSemanticNaming: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val generateFragmentImplementations: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val generateMethods: ListProperty<GeneratedMethod>

  @get:Input
  @get:Optional
  abstract val generateQueryDocument: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val generateSchema: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val generatedSchemaName: Property<String>

  @get:Input
  @get:Optional
  abstract val operationManifestFormat: Property<String>

  /**
   * JavaCodegenOptions
   */
  @get:Input
  @get:Optional
  abstract val generatePrimitiveTypes: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val nullableFieldStyle: Property<JavaNullable>

  @get:Input
  @get:Optional
  abstract val generateModelBuilders: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val classesForEnumsMatching: ListProperty<String>

  /**
   * KotlinCodegenOptions
   */
  @get:Input
  @get:Optional
  abstract val sealedClassesForEnumsMatching: ListProperty<String>

  @get:Input
  @get:Optional
  abstract val generateAsInternal: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val generateFilterNotNull: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val generateInputBuilders: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val addJvmOverloads: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val jsExport: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val requiresOptInAnnotation: Property<String>

  @get:OutputFile
  abstract val codegenOptionsFile: RegularFileProperty

  @TaskAction
  fun taskAction() {

    CodegenSchemaOptions(
        scalarMapping = scalarMapping(scalarTypeMapping, scalarAdapterMapping),
        generateDataBuilders = generateDataBuilders.orNull,
    ).writeTo(codegenSchemaOptionsFile.get().asFile)

    IrOptions(
        codegenModels = codegenModels.orNull,
        addTypename = addTypename.orNull,
        fieldsOnDisjointTypesMustMerge = fieldsOnDisjointTypesMustMerge.orNull,
        decapitalizeFields = decapitalizeFields.orNull,
        flattenModels = flattenModels.orNull,
        warnOnDeprecatedUsages = warnOnDeprecatedUsages.orNull,
        failOnWarnings = failOnWarnings.orNull,
        generateOptionalOperationVariables = generateOptionalOperationVariables.orNull,
        alwaysGenerateTypesMatching = alwaysGenerateTypesMatching.orNull
    ).writeTo(irOptionsFile.get().asFile)

    val codegenOptions = CodegenOptions(
        targetLanguage = targetLanguage.get(),
        generateMethods = generateMethods.orNull,
        generateFragmentImplementations = generateFragmentImplementations.orNull,
        generateQueryDocument = generateQueryDocument.orNull,
        useSemanticNaming = useSemanticNaming.orNull,
        generateSchema = generateSchema.orNull,
        generatedSchemaName = generatedSchemaName.orNull,
        sealedClassesForEnumsMatching = sealedClassesForEnumsMatching.orNull,
        generateAsInternal = generateAsInternal.orNull,
        generateFilterNotNull = generateFilterNotNull.orNull,
        generateInputBuilders = generateInputBuilders.orNull,
        addJvmOverloads = addJvmOverloads.orNull,
        requiresOptInAnnotation = requiresOptInAnnotation.orNull,
        jsExport = jsExport.orNull,
        generateModelBuilders = generateModelBuilders.orNull,
        classesForEnumsMatching = classesForEnumsMatching.orNull,
        generatePrimitiveTypes = generatePrimitiveTypes.orNull,
        nullableFieldStyle = nullableFieldStyle.orNull,
        decapitalizeFields = decapitalizeFields.orNull
    )

    codegenOptions.writeTo(this.codegenOptionsFile.get().asFile)
  }
}

private fun scalarMapping(
    scalarTypeMapping: MapProperty<String, String>,
    scalarAdapterMapping: MapProperty<String, String>,
): Map<String, ScalarInfo> {
  return scalarTypeMapping.getOrElse(emptyMap()).mapValues { (graphQLName, targetName) ->
    val adapterInitializerExpression = scalarAdapterMapping.getOrElse(emptyMap())[graphQLName]
    ScalarInfo(targetName, if (adapterInitializerExpression == null) RuntimeAdapterInitializer else ExpressionAdapterInitializer(adapterInitializerExpression))
  }
}