package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.gradle.api.Service
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import java.io.File

object BuildDirLayout {
  internal fun operationManifest(project: Project, service: Service, format: String): File {
    return project.layout.buildDirectory.file(
        "generated/manifest/apollo/${service.name}/$format.json"
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

  internal fun ir(project: Project, service: Service): Provider<RegularFile> {
    return project.layout.buildDirectory.file(
        "generated/ir/apollo/${service.name}/ir.json"
    )
  }

  internal fun irOptions(project: Project, service: Service): Provider<RegularFile> {
    return project.layout.buildDirectory.file(
        "generated/options/apollo/${service.name}/irOptions.json"
    )
  }

  internal fun codegenOptions(project: Project, service: Service): Provider<RegularFile> {
    return project.layout.buildDirectory.file(
        "generated/options/apollo/${service.name}/codegenOptions.json"
    )
  }

  internal fun otherOptions(project: Project, service: Service): Provider<RegularFile> {
    return project.layout.buildDirectory.file(
        "generated/options/apollo/${service.name}/otherOptions.json"
    )
  }

  internal fun codegenSchemaOptions(project: Project, service: Service): Provider<RegularFile> {
    return project.layout.buildDirectory.file(
        "generated/options/apollo/${service.name}/codegenSchemaOptions.json"
    )
  }
  internal fun codegenSchema(project: Project, service: Service): Provider<RegularFile> {
    return project.layout.buildDirectory.file(
        "generated/schema/apollo/${service.name}/codegenSchema.json"
    )
  }

  internal fun outputDir(project: Project, service: Service): Provider<Directory> {
    return project.layout.buildDirectory.dir(
        "generated/source/apollo/${service.name}"
    )
  }

  internal fun kspProcessorJar(project: Project, serviceName: String): Provider<RegularFile> {
    return project.layout.buildDirectory.file(
        "generated/jar/apollo/${serviceName}Processor.jar"
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
