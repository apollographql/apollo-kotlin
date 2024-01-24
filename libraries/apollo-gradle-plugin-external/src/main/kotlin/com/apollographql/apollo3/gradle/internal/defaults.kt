package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.compiler.MANIFEST_NONE
import com.apollographql.apollo3.compiler.MANIFEST_OPERATION_OUTPUT
import com.apollographql.apollo3.compiler.MANIFEST_PERSISTED_QUERY
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import java.io.File


internal fun ApolloGenerateSourcesBaseTask.setPackageNameProperties(service: DefaultService) {
  packageName.set(service.packageName)
  packageNamesFromFilePaths.set(service.packageNamesFromFilePaths)
  if (service.packageNamesFromFilePaths) {
    packageNameRoots = service.graphqlSourceDirectorySet.srcDirs.map { it.absolutePath }.toSet()
  }
  packageNameGenerator = service.packageNameGenerator.orNull
}


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

