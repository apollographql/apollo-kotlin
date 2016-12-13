package com.apollostack.android.gradle

import com.apollostack.compiler.GraphQLCompiler
import com.apollostack.android.VERSION
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import java.io.File

open class ApolloTask : SourceTask() {
  @Suppress("unused") // Required to invalidate the task on version updates.
  @Input fun pluginVersion() = VERSION

  @get:OutputDirectory var outputDirectory: File? = null

  var buildDirectory: File? = null
    set(value) {
      field = value
      outputDirectory = GraphQLCompiler.OUTPUT_DIRECTORY.fold(buildDirectory, ::File)
    }

  @TaskAction
  fun execute(inputs: IncrementalTaskInputs) {

  }
}
