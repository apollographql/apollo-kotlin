package com.apollographql.apollo.gradle.task

import com.apollographql.apollo.compiler.CodegenOptions
import com.apollographql.apollo.compiler.CodegenSchemaOptions
import com.apollographql.apollo.compiler.GeneratedMethod
import com.apollographql.apollo.compiler.IrOptions
import com.apollographql.apollo.compiler.IssueSeverity
import com.apollographql.apollo.compiler.JavaNullable
import com.apollographql.apollo.compiler.MANIFEST_NONE
import com.apollographql.apollo.compiler.MANIFEST_OPERATION_OUTPUT
import com.apollographql.apollo.compiler.MANIFEST_PERSISTED_QUERY
import com.apollographql.apollo.compiler.MODELS_OPERATION_BASED
import com.apollographql.apollo.compiler.MODELS_OPERATION_BASED_WITH_INTERFACES
import com.apollographql.apollo.compiler.MODELS_RESPONSE_BASED
import com.apollographql.apollo.compiler.TargetLanguage
import com.apollographql.apollo.compiler.writeTo
import gratatouille.tasks.GInputFiles
import gratatouille.tasks.GOutputFile
import gratatouille.tasks.GTask
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@GTask
internal fun apolloGenerateOptions(
    generateKotlinModels: Boolean?,
    languageVersion: String?,
    scalarTypeMapping: Map<String, String>?,
    scalarAdapterMapping: Map<String, String>?,
    codegenModels: String?,
    generateDataBuilders: Boolean?,
    addTypename: String?,
    decapitalizeFields: Boolean?,
    flattenModels: Boolean?,
    failOnWarnings: Boolean?,
    generateOptionalOperationVariables: Boolean?,
    alwaysGenerateTypesMatching: Set<String>?,
    packageName: String?,
    rootPackageName: String?,
    useSemanticNaming: Boolean?,
    generateFragmentImplementations: Boolean?,
    generateMethods: List<String>?,
    generateQueryDocument: Boolean?,
    generateSchema: Boolean?,
    generatedSchemaName: String?,
    operationManifestFormat: String?,
    severities: Map<String, String>?,
    allowFragmentArguments: Boolean?,
    // JavaCodegenOptions
    generatePrimitiveTypes: Boolean?,
    nullableFieldStyle: String?,
    generateModelBuilders: Boolean?,
    classesForEnumsMatching: List<String>?,
    // KotlinCodegenOptions
    sealedClassesForEnumsMatching: List<String>?,
    generateApolloEnums: Boolean?,
    generateAsInternal: Boolean?,
    generateInputBuilders: Boolean?,
    addJvmOverloads: Boolean?,
    jsExport: Boolean?,
    requiresOptInAnnotation: String?,

    // multi modules
    upstreamOtherOptions: GInputFiles,
    javaPluginApplied: Boolean,
    kgpVersion: String?,
    kmp: Boolean,
    generateAllTypes: Boolean,

    // outputs
    codegenSchemaOptionsFile: GOutputFile,
    irOptionsFile: GOutputFile,
    codegenOptions: GOutputFile,
    otherOptions: GOutputFile,
) {
  check(
      packageName != null || rootPackageName != null
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

  val upstreamOtherOptions = upstreamOtherOptions.firstOrNull()?.file?.toOtherOptions()
  val upstreamTargetLanguage = upstreamOtherOptions?.targetLanguage
  val targetLanguage =
    targetLanguage(generateKotlinModels, languageVersion, javaPluginApplied, kgpVersion, upstreamTargetLanguage)
  val generateFilterNotNull = generateFilterNotNull(targetLanguage, kmp)
  val alwaysGenerateTypesMatching =
    alwaysGenerateTypesMatching(alwaysGenerateTypesMatching, generateAllTypes)
  val upstreamCodegenModels = upstreamOtherOptions?.codegenModels
  val codegenModels = codegenModels(codegenModels, upstreamCodegenModels)

  CodegenSchemaOptions(
      scalarTypeMapping = scalarTypeMapping ?: emptyMap(),
      scalarAdapterMapping = scalarAdapterMapping ?: emptyMap(),
      generateDataBuilders = generateDataBuilders ?: false,
  ).writeTo(codegenSchemaOptionsFile)

  IrOptions(
      codegenModels = codegenModels,
      addTypename = addTypename,
      decapitalizeFields = decapitalizeFields,
      flattenModels = flattenModels,
      failOnWarnings = failOnWarnings,
      generateOptionalOperationVariables = generateOptionalOperationVariables,
      alwaysGenerateTypesMatching = alwaysGenerateTypesMatching,
      issueSeverities = severities?.convert(),
      allowFragmentArguments = allowFragmentArguments
  ).writeTo(irOptionsFile)

  CodegenOptions(
      targetLanguage = targetLanguage,
      useSemanticNaming = useSemanticNaming,
      generateFragmentImplementations = generateFragmentImplementations,
      generateMethods = generateMethods?.map { GeneratedMethod.fromName(it) ?: error("Apollo: unknown method type: $it for generateMethods")},
      generateQueryDocument = generateQueryDocument,
      generateSchema = generateSchema,
      generatedSchemaName = generatedSchemaName,
      operationManifestFormat = operationManifestFormat(operationManifestFormat),
      nullableFieldStyle = nullableFieldStyle?.let { JavaNullable.fromName(it) },
      generateModelBuilders = generateModelBuilders,
      classesForEnumsMatching = classesForEnumsMatching,
      generatePrimitiveTypes = generatePrimitiveTypes,
      generateAsInternal = generateAsInternal,
      generateFilterNotNull = generateFilterNotNull,
      sealedClassesForEnumsMatching = sealedClassesForEnumsMatching,
      addJvmOverloads = addJvmOverloads,
      requiresOptInAnnotation = requiresOptInAnnotation,
      jsExport = jsExport,
      generateInputBuilders = generateInputBuilders,
      decapitalizeFields = decapitalizeFields,
      addDefaultArgumentForInputObjects = true,
      addUnknownForEnums = true,
      generateApolloEnums = generateApolloEnums,
      packageName = packageName,
      rootPackageName = rootPackageName
  ).writeTo(codegenOptions)

  OtherOptions(targetLanguage, codegenModels).writeTo(otherOptions)
}

@Suppress("DEPRECATION_ERROR")
private fun operationManifestFormat(format: String?): String {
  if (format == null) {
    return MANIFEST_PERSISTED_QUERY
  } else {
    when (format) {
      MANIFEST_PERSISTED_QUERY,
      MANIFEST_NONE,
        -> return format
      MANIFEST_OPERATION_OUTPUT -> {
        error("Apollo: using '$MANIFEST_OPERATION_OUTPUT' is deprecated, please use '$MANIFEST_PERSISTED_QUERY' instead")
      }

      else -> {
        error("Apollo: unknown operation manifest format: $format")
      }
    }
  }
}

private fun Map<String, String>.convert(): Map<String, IssueSeverity> {
  return mapValues {
    when (it.value) {
      "ignore" -> IssueSeverity.Ignore
      "warn" -> IssueSeverity.Warn
      "error" -> IssueSeverity.Error
      else -> error("Unknown severity '${it.value}'. Expected one of: 'ignore', 'warn', 'error'")
    }
  }
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

private fun targetLanguage(
    generateKotlinModels: Boolean?,
    languageVersion: String?,
    javaPluginApplied: Boolean,
    kgpVersion: String?,
    upstreamTargetLanguage: TargetLanguage?,
): TargetLanguage {
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

@Serializable
internal class OtherOptions(
    val targetLanguage: TargetLanguage,
    val codegenModels: String,
)

internal fun OtherOptions.writeTo(file: File) {
  file.writeText(Json.encodeToString(this))
}

internal fun File.toOtherOptions(): OtherOptions {
  return Json.decodeFromString(readText())
}

fun getKotlinTargetLanguage(kgpVersion: String, userSpecified: String?): TargetLanguage {
  @Suppress("DEPRECATION_ERROR")
  return when (userSpecified) {
    "1.5" -> TargetLanguage.KOTLIN_1_5
    "1.9" -> TargetLanguage.KOTLIN_1_9
    null -> {
      // User didn't specify a version: default to the Kotlin plugin version
      val kotlinPluginVersion = kgpVersion.split("-")[0]
      val versionNumbers = kotlinPluginVersion.split(".").map { it.toInt() }
      val version = KotlinVersion(versionNumbers[0], versionNumbers[1])
      if (version.isAtLeast(1, 9)) {
        TargetLanguage.KOTLIN_1_9
      } else {
        TargetLanguage.KOTLIN_1_5
      }
    }

    else -> error("Apollo: languageVersion '$userSpecified' is not supported, Supported values: '1.5', '1.9'")
  }
}

internal fun alwaysGenerateTypesMatching(alwaysGenerateTypesMatching: Set<String>?, generateAllTypes: Boolean): Set<String> {
  if (alwaysGenerateTypesMatching != null) {
    // The user specified something, use this
    return alwaysGenerateTypesMatching
  }

  if (generateAllTypes) {
    return setOf(".*")
  } else {
    // get the used coordinates from the downstream dependencies
    return emptySet()
  }
}