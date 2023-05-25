package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.compiler.MANIFEST_OPERATION_OUTPUT
import com.apollographql.apollo3.gradle.api.Service
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import java.io.File

object BuildDirLayout {
  internal fun operationManifest(project: Project, service: Service, format: String): File {
    val dir = when (format) {
      MANIFEST_OPERATION_OUTPUT -> "operationOutput"
      else -> "manifest"
    }
    return project.layout.buildDirectory.file(
        "generated/$dir/apollo/${service.name}/$format.json"
    ).get().asFile
  }

  internal fun metadata(project: Project, service: Service): Provider<RegularFile> {
    return project.layout.buildDirectory.file(
        "generated/metadata/apollo/${service.name}/metadata.json"
    )
  }

  internal fun usedCoordinates(project: Project, service: Service): Provider<RegularFile> {
    return project.layout.buildDirectory.file(
        "generated/usedCoordinates/apollo/${service.name}/usedCoordinates.json"
    )
  }

  internal fun schema(project: Project, service: Service): Provider<RegularFile> {
    return project.layout.buildDirectory.file(
        "generated/schema/apollo/${service.name}/schema.graphqls"
    )
  }

  internal fun outputDir(project: Project, service: Service): Provider<Directory> {
    return project.layout.buildDirectory.dir(
        "generated/source/apollo/${service.name}"
    )
  }

  internal fun testDir(project: Project, service: Service): Provider<Directory> {
    return project.layout.buildDirectory.dir(
        "generated/source/apolloTest/${service.name}"
    )
  }

  internal fun versionCheck(project: Project): Provider<RegularFile> {
    return project.layout.buildDirectory.file(
        "generated/checks/apollo/versionCheck"
    )
  }

  internal fun legacyJsTargetCheck(project: Project): Provider<RegularFile> {
    return project.layout.buildDirectory.file(
        "generated/checks/apollo/legacyJsTargetCheck"
    )
  }

  internal fun duplicatesCheck(project: Project, service: Service): Provider<RegularFile> {
    return project.layout.buildDirectory.file(
        "generated/checks/apollo/${service.name}/duplicatesCheck"
    )
  }
}
