package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.compiler.CodegenOptions
import com.apollographql.apollo.compiler.CodegenSchemaOptions
import com.apollographql.apollo.compiler.ExpressionAdapterInitializer
import com.apollographql.apollo.compiler.GeneratedMethod
import com.apollographql.apollo.compiler.IrOptions
import com.apollographql.apollo.compiler.JavaNullable
import com.apollographql.apollo.compiler.MANIFEST_OPERATION_OUTPUT
import com.apollographql.apollo.compiler.MANIFEST_PERSISTED_QUERY
import com.apollographql.apollo.compiler.MODELS_OPERATION_BASED
import com.apollographql.apollo.compiler.MODELS_OPERATION_BASED_WITH_INTERFACES
import com.apollographql.apollo.compiler.MODELS_RESPONSE_BASED
import com.apollographql.apollo.compiler.RuntimeAdapterInitializer
import com.apollographql.apollo.compiler.ScalarInfo
import com.apollographql.apollo.compiler.TargetLanguage
import com.apollographql.apollo.compiler.writeTo
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

@CacheableTask
abstract class ApolloGenerateOptionsTask : DefaultTask() {
  /**
   * CodegenSchemaOptions
   */
  @get:Input
  @get:Optional
  abstract val generateKotlinModels: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val languageVersion: Property<String>

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
  abstract val packageName: Property<String>

  @get:Input
  @get:Optional
  abstract val rootPackageName: Property<String>

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
  abstract val codegenOptions: RegularFileProperty

  /**
   * Gradle model
   */
  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val upstreamOtherOptions: ConfigurableFileCollection

  @get:Input
  abstract var isJavaPluginApplied: Boolean

  @get:Input
  @get:Optional
  abstract var kgpVersion: String?

  @get:Input
  abstract var isKmp: Boolean

  @get:Input
  abstract var generateAllTypes: Boolean

  @get:Internal
  var hasPackageNameGenerator: Boolean = false

  @get:OutputFile
  abstract val otherOptions: RegularFileProperty

  @TaskAction
  fun taskAction() {
    check(
        packageName.isPresent || rootPackageName.isPresent || hasPackageNameGenerator
    ) {
      """
            |Apollo: specify 'packageName':
            |apollo {
            |  service("service") {
            |    packageName.set("com.example")
            |  }
            |}
          """.trimMargin()
    }

    val upstreamOtherOptions = upstreamOtherOptions.firstOrNull()?.toOtherOptions()
    val upstreamTargetLanguage = upstreamOtherOptions?.targetLanguage
    val targetLanguage = targetLanguage(generateKotlinModels.orNull, languageVersion.orNull, isJavaPluginApplied, kgpVersion, upstreamTargetLanguage)
    val generateFilterNotNull = generateFilterNotNull(targetLanguage, isKmp)
    val alwaysGenerateTypesMatching = alwaysGenerateTypesMatching(alwaysGenerateTypesMatching.orNull, generateAllTypes)
    val upstreamCodegenModels = upstreamOtherOptions?.codegenModels
    val codegenModels = codegenModels(codegenModels.orNull, upstreamCodegenModels)

    CodegenSchemaOptions(
        scalarMapping = scalarMapping(scalarTypeMapping, scalarAdapterMapping),
        generateDataBuilders = generateDataBuilders.orNull,
    ).writeTo(codegenSchemaOptionsFile.get().asFile)

    IrOptions(
        codegenModels = codegenModels,
        addTypename = addTypename.orNull,
        fieldsOnDisjointTypesMustMerge = fieldsOnDisjointTypesMustMerge.orNull,
        decapitalizeFields = decapitalizeFields.orNull,
        flattenModels = flattenModels.orNull,
        warnOnDeprecatedUsages = warnOnDeprecatedUsages.orNull,
        failOnWarnings = failOnWarnings.orNull,
        generateOptionalOperationVariables = generateOptionalOperationVariables.orNull,
        alwaysGenerateTypesMatching = alwaysGenerateTypesMatching
    ).writeTo(irOptionsFile.get().asFile)

    CodegenOptions(
        targetLanguage = targetLanguage,
        useSemanticNaming = useSemanticNaming.orNull,
        generateFragmentImplementations = generateFragmentImplementations.orNull,
        generateMethods = generateMethods.orNull,
        generateQueryDocument = generateQueryDocument.orNull,
        generateSchema = generateSchema.orNull,
        generatedSchemaName = generatedSchemaName.orNull,
        operationManifestFormat = operationManifestFormat.orNull,
        nullableFieldStyle = nullableFieldStyle.orNull,
        generateModelBuilders = generateModelBuilders.orNull,
        classesForEnumsMatching = classesForEnumsMatching.orNull,
        generatePrimitiveTypes = generatePrimitiveTypes.orNull,
        generateAsInternal = generateAsInternal.orNull,
        generateFilterNotNull = generateFilterNotNull,
        sealedClassesForEnumsMatching = sealedClassesForEnumsMatching.orNull,
        addJvmOverloads = addJvmOverloads.orNull,
        requiresOptInAnnotation = requiresOptInAnnotation.orNull,
        jsExport = jsExport.orNull,
        generateInputBuilders = generateInputBuilders.orNull,
        decapitalizeFields = decapitalizeFields.orNull,
        addDefaultArgumentForInputObjects = true,
        addUnknownForEnums = true,
        packageName = packageName.orNull,
        rootPackageName = rootPackageName.orNull
    ).writeTo(codegenOptions.get().asFile)

    OtherOptions(targetLanguage, codegenModels).writeTo(otherOptions.get().asFile)
  }

  private fun codegenModels(codegenModels: String?, upstreamCodegenModels: String?): String {
    if (codegenModels != null) {
      setOf(MODELS_OPERATION_BASED, MODELS_RESPONSE_BASED, MODELS_OPERATION_BASED_WITH_INTERFACES).apply {
        check(contains(codegenModels)) {
          "Apollo: unknown codegenModels '$codegenModels'. Valid values: $this"
        }
      }

      check(upstreamCodegenModels == null || codegenModels == upstreamCodegenModels) {
        "Apollo: Expected '$upstreamCodegenModels', got '$codegenModels'. Check your codegenModels setting."
      }
      return codegenModels
    }
    if (upstreamCodegenModels != null) {
      return upstreamCodegenModels
    }

    return MODELS_OPERATION_BASED
  }
}

private fun targetLanguage(generateKotlinModels: Boolean?,
                           languageVersion: String?,
                           javaPluginApplied: Boolean,
                           kgpVersion: String?,
                           upstreamTargetLanguage: TargetLanguage?): TargetLanguage {
    return when {
      generateKotlinModels != null -> {
        if (generateKotlinModels) {
          check(kgpVersion != null) {
            "Apollo: generateKotlinModels.set(true) requires to apply a Kotlin plugin"
          }
          val targetLanguage = getKotlinTargetLanguage(kgpVersion, languageVersion)

          check(upstreamTargetLanguage == null || targetLanguage == upstreamTargetLanguage) {
            "Apollo: Expected '$upstreamTargetLanguage', got '$targetLanguage'. Check your generateKotlinModels and languageVersion settings."
          }
          targetLanguage
        } else {
          check(javaPluginApplied) {
            "Apollo: generateKotlinModels.set(false) requires to apply the Java plugin"
          }

          check(upstreamTargetLanguage == null || TargetLanguage.JAVA == upstreamTargetLanguage) {
            "Apollo: Expected '$upstreamTargetLanguage', got '${TargetLanguage.JAVA}'. Check your generateKotlinModels settings."
          }

          TargetLanguage.JAVA
        }
      }

      upstreamTargetLanguage != null -> {
        upstreamTargetLanguage
      }
      kgpVersion != null -> {
        getKotlinTargetLanguage(kgpVersion, languageVersion)
      }

      javaPluginApplied -> {
        TargetLanguage.JAVA
      }
      else -> {
        error("Apollo: No Java or Kotlin plugin found")
      }
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

@Serializable
internal class OtherOptions(
    val targetLanguage: TargetLanguage,
    val codegenModels: String
)

internal fun OtherOptions.writeTo(file: File) {
  file.writeText(Json.encodeToString(this))
}

internal fun File.toOtherOptions(): OtherOptions {
  return Json.decodeFromString(readText())
}