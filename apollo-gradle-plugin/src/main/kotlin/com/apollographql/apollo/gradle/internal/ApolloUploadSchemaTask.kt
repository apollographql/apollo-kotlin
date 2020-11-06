package com.apollographql.apollo.gradle.internal

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

abstract class ApolloUploadSchemaTask : DefaultTask() {
  @get:Optional
  @get:Input
  @get:Option(option = "endpoint", description = "url of the GraphQL endpoint")
  abstract val endpoint: Property<String>

  @get:Input
  @get:Optional
  @get:Option(option = "schema", description = "path where the schema will be downloaded, relative to the current working directory")
  abstract val schema: Property<String>

  @TaskAction
  fun taskAction() {


  }
}