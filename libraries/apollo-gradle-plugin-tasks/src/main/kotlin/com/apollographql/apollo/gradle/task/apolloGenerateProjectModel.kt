package com.apollographql.apollo.gradle.task

import com.apollographql.apollo.compiler.model.ProjectModel
import com.apollographql.apollo.compiler.model.writeTo
import gratatouille.tasks.GOutputFile
import gratatouille.tasks.GTask

@GTask
internal fun apolloGenerateProjectModel(
    // Inputs
    serviceNames: Set<String>,

    // Outputs
    projectModelFile: GOutputFile,
) {
  ProjectModel(
      serviceNames = serviceNames,
  )
      .writeTo(projectModelFile)
}
