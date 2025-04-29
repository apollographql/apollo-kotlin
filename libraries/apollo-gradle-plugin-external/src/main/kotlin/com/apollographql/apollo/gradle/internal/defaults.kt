@file:Suppress("DEPRECATION")

package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.compiler.MANIFEST_NONE
import com.apollographql.apollo.compiler.MANIFEST_OPERATION_OUTPUT
import com.apollographql.apollo.compiler.MANIFEST_PERSISTED_QUERY
import com.apollographql.apollo.compiler.TargetLanguage
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import java.io.File


/**
 * Resolves the operation manifest and formats.
 */
private fun DefaultService.resolveOperationManifest(): Pair<String, File?> {
  @Suppress("DEPRECATION_ERROR")
  generateOperationOutput.disallowChanges()
  @Suppress("DEPRECATION_ERROR")
  operationOutputFile.disallowChanges()
  operationManifest.disallowChanges()
  operationManifestFormat.disallowChanges()

  var format = operationManifestFormat.orNull
  if (format == null) {
    @Suppress("DEPRECATION_ERROR")
    if (generateOperationOutput.orElse(false).get()) {
      println("Apollo: using 'generateOperationOutput' is deprecated, please use 'operationManifestFormat.set(\"$MANIFEST_PERSISTED_QUERY\")' instead")
      format = MANIFEST_OPERATION_OUTPUT
    }
  } else {
    @Suppress("DEPRECATION_ERROR")
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
    @Suppress("DEPRECATION_ERROR")
    check(!generateOperationOutput.isPresent) {
      "Apollo: it is an error to set both `generateOperationOutput` and `operationManifestFormat`. Remove `generateOperationOutput`"
    }
  }
  var userFile = operationManifest.orNull?.asFile
  if (userFile == null) {
    @Suppress("DEPRECATION_ERROR")
    userFile = operationOutputFile.orNull?.asFile
  } else {
    @Suppress("DEPRECATION_ERROR")
    check(!operationOutputFile.isPresent) {
      "Apollo: it is an error to set both `operationManifest` and `operationOutputFile`. Remove `operationOutputFile`"
    }
  }

  if (userFile != null) {
    if (format == null) {
      @Suppress("DEPRECATION_ERROR")
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