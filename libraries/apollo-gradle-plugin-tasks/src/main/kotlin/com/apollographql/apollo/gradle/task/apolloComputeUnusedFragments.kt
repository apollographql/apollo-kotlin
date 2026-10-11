package com.apollographql.apollo.gradle.task

import com.apollographql.apollo.compiler.EntryPoints
import gratatouille.tasks.GInputFile
import gratatouille.tasks.GInputFiles
import gratatouille.tasks.GOutputFile
import gratatouille.tasks.GTask

@GTask
internal fun apolloComputeUnusedFragments(
    irOperations: GInputFile,
    downstreamIrOperations: GInputFiles,
    downstreamFragmentUsageIsComplete: Boolean,
    outputFile: GOutputFile,
) {
  EntryPoints.computeUnusedFragments(
      irOperations = irOperations,
      downstreamIrOperations = downstreamIrOperations.toInputFiles(),
      downstreamFragmentUsageIsComplete = downstreamFragmentUsageIsComplete,
      outputFile = outputFile,
  )
}
