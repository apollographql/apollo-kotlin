@file:Suppress("DEPRECATION")

package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.compiler.MANIFEST_NONE
import com.apollographql.apollo.compiler.MANIFEST_OPERATION_OUTPUT
import com.apollographql.apollo.compiler.MANIFEST_PERSISTED_QUERY
import com.apollographql.apollo.compiler.OperationIdGenerator
import com.apollographql.apollo.compiler.OperationOutputGenerator
import com.apollographql.apollo.compiler.ApolloCompilerPlugin
import com.apollographql.apollo.compiler.TargetLanguage
import com.apollographql.apollo.compiler.operationoutput.OperationDescriptor
import com.apollographql.apollo.compiler.operationoutput.OperationId
import com.apollographql.apollo.compiler.operationoutput.OperationOutput
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import java.io.File


/**
 * Resolves the operation manifest and formats.
 */
@Suppress("DEPRECATION")
private fun DefaultService.resolveOperationManifest(): Pair<String, File?> {
  generateOperationOutput.disallowChanges()
  operationOutputFile.disallowChanges()
  operationManifest.disallowChanges()
  operationManifestFormat.disallowChanges()

  var format = operationManifestFormat.orNull
  if (format == null) {
    if (generateOperationOutput.orElse(false).get()) {
      println("Apollo: using 'generateOperationOutput' is deprecated, please use 'operationManifestFormat.set(\"$MANIFEST_PERSISTED_QUERY\")' instead")
      format = MANIFEST_OPERATION_OUTPUT
    }
  } else {
    when (format) {
      MANIFEST_NONE,
      MANIFEST_PERSISTED_QUERY,
      -> Unit
      MANIFEST_OPERATION_OUTPUT -> {
        println("Apollo: using '$MANIFEST_OPERATION_OUTPUT' is deprecated, please use '$MANIFEST_PERSISTED_QUERY' instead")
      }

      else -> {
        error("Apollo: unknown operation manifest format: $format")
      }
    }
    check(!generateOperationOutput.isPresent) {
      "Apollo: it is an error to set both `generateOperationOutput` and `operationManifestFormat`. Remove `generateOperationOutput`"
    }
  }
  var userFile = operationManifest.orNull?.asFile
  if (userFile == null) {
    userFile = operationOutputFile.orNull?.asFile
  } else {
    check(!operationOutputFile.isPresent) {
      "Apollo: it is an error to set both `operationManifest` and `operationOutputFile`. Remove `operationOutputFile`"
    }
  }

  if (userFile != null) {
    if (format == null) {
      format = MANIFEST_OPERATION_OUTPUT
    }
  } else {
    userFile = if (format == null || format == MANIFEST_NONE) {
      null
    } else {
      BuildDirLayout.operationManifest(project, this, format)
    }
  }

  if (format == null) {
    format = MANIFEST_NONE
  }
  return format to userFile
}

internal fun DefaultService.operationManifestFile(): RegularFileProperty {
  return project.provider {
    resolveOperationManifest().second
  }.let { fileProvider ->
    project.objects.fileProperty().fileProvider(fileProvider)
  }
}

internal fun DefaultService.operationManifestFormat(): Provider<String> {
  return project.provider {
    resolveOperationManifest().first
  }
}

internal fun ApolloCompilerPlugin.toOperationOutputGenerator(): OperationOutputGenerator {
  return object : OperationOutputGenerator {
    override fun generate(operationDescriptorList: Collection<OperationDescriptor>): OperationOutput {
      var operationIds = operationIds(operationDescriptorList.toList())
      if (operationIds == null) {
        operationIds = operationDescriptorList.map { OperationId(OperationIdGenerator.Sha256.apply(it.source, it.name), it.name) }
      }
      return operationDescriptorList.associateBy { descriptor ->
        val operationId = operationIds.firstOrNull { it.name == descriptor.name } ?: error("No id found for operation ${descriptor.name}")
        operationId.id
      }
    }
  }
}

internal fun generateFilterNotNull(targetLanguage: TargetLanguage, isKmp: Boolean): Boolean? {
  return if (targetLanguage == TargetLanguage.JAVA) {
    null
  } else {
    isKmp
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