package com.apollographql.apollo.gradle.task

import com.apollographql.apollo.compiler.UsedCoordinates
import com.apollographql.apollo.compiler.toIrOperations
import com.apollographql.apollo.compiler.writeTo
import gratatouille.GInputFiles
import gratatouille.GOutputFile
import gratatouille.GTask

@GTask
fun apolloComputeUsedCoordinates(
    irOperations: GInputFiles,
    outputFile: GOutputFile,
) {
  val usedCoordinates: UsedCoordinates = irOperations.map {
    it.file.toIrOperations().usedCoordinates
  }.fold(UsedCoordinates()) { acc, element ->
    acc.mergeWith(element)
  }

  usedCoordinates.writeTo(outputFile)
}
