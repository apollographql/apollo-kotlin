package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.compiler.MANIFEST_NONE
import com.apollographql.apollo3.compiler.MANIFEST_OPERATION_OUTPUT
import com.apollographql.apollo3.compiler.MANIFEST_PERSISTED_QUERY
import com.apollographql.apollo3.compiler.OperationIdGenerator
import com.apollographql.apollo3.compiler.OperationOutputGenerator
import com.apollographql.apollo3.compiler.Plugin
import com.apollographql.apollo3.compiler.operationoutput.OperationDescriptor
import com.apollographql.apollo3.compiler.operationoutput.OperationId
import com.apollographql.apollo3.compiler.operationoutput.OperationOutput
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
      format = MANIFEST_OPERATION_OUTPUT
    }
  } else {
    when (format) {
      MANIFEST_NONE,
      MANIFEST_OPERATION_OUTPUT,
      MANIFEST_PERSISTED_QUERY,
      -> Unit

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

internal fun Plugin.toOperationOutputGenerator(): OperationOutputGenerator {
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