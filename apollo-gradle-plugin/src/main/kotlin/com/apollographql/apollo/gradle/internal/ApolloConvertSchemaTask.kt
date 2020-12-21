package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.compiler.frontend.GraphQLParser
import com.apollographql.apollo.compiler.frontend.toFile
import com.apollographql.apollo.compiler.frontend.toIntrospectionSchema
import com.apollographql.apollo.compiler.frontend.toSchema
import com.apollographql.apollo.compiler.frontend.toUtf8WithIndents
import com.apollographql.apollo.compiler.introspection.IntrospectionSchema
import com.apollographql.apollo.compiler.introspection.IntrospectionSchema.Companion.wrap
import com.apollographql.apollo.compiler.toJson
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.io.File

abstract class ApolloConvertSchemaTask: DefaultTask() {
  @get:Input
  @get:Option(option = "from", description = "schema to convert from")
  abstract val from: Property<String>

  // Even if this is points to an output file, for the purpose of the task this is seen as an input
  @get:Input
  @get:Option(option = "to", description = "schema to convert to")
  abstract val to: Property<String>

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
    check (from.isIntrospection() && !to.isIntrospection() || !from.isIntrospection() && to.isIntrospection()) {
      "Cannot convert from ${from.name} to ${to.name}, they are already the same format"
    }

    if (from.isIntrospection()) {
      IntrospectionSchema(from).toSchema().toDocument().toUtf8WithIndents().let {
        to.writeText(it)
      }
    } else {
      GraphQLParser.parseSchema(from).toIntrospectionSchema().wrap().toJson(to)
    }
  }

  @TaskAction
  fun taskAction() {
    // Do not use Project.file() as from the command line, we expect the file to be resolved based on cwd
    convert(File(from.get()), File(to.get()))
  }
}
