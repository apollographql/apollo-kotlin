package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.ast.introspection.toGQLDocument
import com.apollographql.apollo.ast.introspection.toIntrospectionSchema
import com.apollographql.apollo.ast.introspection.writeTo
import com.apollographql.apollo.ast.toFullSchemaGQLDocument
import com.apollographql.apollo.ast.toGQLDocument
import com.apollographql.apollo.ast.toUtf8
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.io.File

abstract class ApolloConvertSchemaTask : DefaultTask() {
  @get:Input
  @get:Option(option = "from", description = "schema to convert from")
  abstract val from: Property<String>

  // Even if this is points to an output file, for the purpose of the task this is seen as an input
  @get:Input
  @get:Option(option = "to", description = "schema to convert to")
  abstract val to: Property<String>

  @get:Internal
  abstract var projectRootDir: String

  init {
    /**
     * This task is really meant to be called from the command line so don't do any up-to-date checks
     * If someone wants to register its own conversion task, it should be easy to do using the
     * compiler APIs directly (see [convert] below)
     * This code actually redundant because the task has no output but adding it make it explicit.
     */
    outputs.upToDateWhen { false }
    outputs.cacheIf { false }
  }

  private fun File.isIntrospection() = extension == "json"

  fun convert(from: File, to: File) {
    check(from.isIntrospection() && !to.isIntrospection() || !from.isIntrospection() && to.isIntrospection()) {
      "Cannot convert from ${from.name} to ${to.name}, they are already the same format"
    }

    if (from.isIntrospection()) {
      from.toIntrospectionSchema().toGQLDocument().toUtf8(to)
    } else {
      from.toGQLDocument().toFullSchemaGQLDocument().toIntrospectionSchema().writeTo(to)
    }
  }

  @TaskAction
  fun taskAction() {
    // Files are relative to the root project. It is not possible in a consistent way to have them relative to the current
    // working directory where the gradle command was started
    convert(File(projectRootDir).resolve(from.get()), File(projectRootDir).resolve(to.get()))
  }
}
