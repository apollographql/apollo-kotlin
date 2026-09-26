package com.apollographql.apollo.gradle.task

import com.apollographql.apollo.compiler.UsedCoordinates
import com.apollographql.apollo.compiler.ir.IrOperations
import com.apollographql.apollo.compiler.ir.computeUsedFragmentNames
import com.apollographql.apollo.compiler.toIrOperations
import com.apollographql.apollo.compiler.toUsedFragmentNames
import com.apollographql.apollo.compiler.writeTo
import gratatouille.tasks.GInputFiles
import gratatouille.tasks.GOutputFile
import gratatouille.tasks.GTask

@GTask
internal fun apolloComputeUsedCoordinates(
    irOperations: GInputFiles,
    outputFile: GOutputFile,
) {
  val allIrOperations = irOperations.map { it.file.toIrOperations() }

  computeUsedCoordinates(allIrOperations).writeTo(outputFile)
}

@GTask
internal fun apolloComputeUsedCoordinatesAndFragmentNames(
    irOperations: GInputFiles,
    outputFile: GOutputFile,
    usedFragmentNamesOutputFile: GOutputFile,
) {
  val allIrOperations = irOperations.map { it.file.toIrOperations() }

  computeUsedCoordinates(allIrOperations).writeTo(outputFile)

  val usedFragmentNames = computeUsedFragmentNames(allIrOperations)
  usedFragmentNames.toUsedFragmentNames().writeTo(usedFragmentNamesOutputFile)
}

private fun computeUsedCoordinates(allIrOperations: List<IrOperations>): UsedCoordinates {
  return allIrOperations.fold(UsedCoordinates()) { acc, element ->
    acc.mergeWith(element.usedCoordinates)
  }
}
