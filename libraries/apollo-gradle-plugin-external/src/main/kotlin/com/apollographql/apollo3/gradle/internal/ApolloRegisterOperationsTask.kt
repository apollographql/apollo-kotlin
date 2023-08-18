package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.compiler.MANIFEST_OPERATION_OUTPUT
import com.apollographql.apollo3.compiler.MANIFEST_PERSISTED_QUERY
import com.apollographql.apollo3.compiler.operationoutput.toOperationOutput
import com.apollographql.apollo3.compiler.pqm.toPersistedQueryManifest
import com.apollographql.apollo3.tooling.CannotModifyOperationBody
import com.apollographql.apollo3.tooling.GraphNotFound
import com.apollographql.apollo3.tooling.PermissionError
import com.apollographql.apollo3.tooling.PersistedQuery
import com.apollographql.apollo3.tooling.PublishOperationsSuccess
import com.apollographql.apollo3.tooling.RegisterOperations
import com.apollographql.apollo3.tooling.publishOperations
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

abstract class ApolloRegisterOperationsTask: DefaultTask() {
  @get:InputFile
  @get:Optional
  abstract val operationOutput: RegularFileProperty

  @get:Input
  @get:Optional
  abstract val operationManifestFormat: Property<String>

  @get:Input
  @get:Optional
  abstract val listId: Property<String>

  @get:Input
  abstract val key: Property<String>

  @get:Input
  abstract val graph: Property<String>

  @get:Input
  @get:Optional
  abstract val graphVariant: Property<String>

  @TaskAction
  fun taskAction() {
    if (listId.isPresent) {
      check(operationManifestFormat.get() == MANIFEST_PERSISTED_QUERY) {
        """Apollo: registering operations to a persisted query list requires operationManifestFormat = "$MANIFEST_PERSISTED_QUERY":
          |apollo {
          |  service("service") {
          |    operationManifestFormat.set("$MANIFEST_PERSISTED_QUERY")
          |  }
          |}
        """.trimMargin()
      }
      val result = publishOperations(
          listId = listId.get(),
          persistedQueries = operationOutput.get().asFile.toPersistedQueryManifest().operations.map {
            PersistedQuery(
                name = it.name,
                id = it.id,
                body = it.body,
                operationType = it.type
            )
          },
          apolloKey = key.get(),
          graph = graph.get()
      )

      when(result) {
        is PublishOperationsSuccess -> {
          println("Apollo: persisted query list uploaded successfully")
        }

        is CannotModifyOperationBody -> error("Cannot upload persisted query list: cannot modify operation body ('${result.message}')")
        GraphNotFound ->  error("Cannot upload persisted query list: graph '$graph' not found")
        is PermissionError -> error("Cannot upload persisted query list: permission error ('${result.message}')")
      }
    } else {
      println("Apollo: registering operations without a listId is deprecated")
      check(operationManifestFormat.get() == MANIFEST_OPERATION_OUTPUT) {
        """Apollo: registering legacy operations requires operationManifestFormat = "$MANIFEST_OPERATION_OUTPUT":
          |apollo {
          |  service("service") {
          |    operationManifestFormat.set("$MANIFEST_OPERATION_OUTPUT")
          |  }
          |}
        """.trimMargin()
      }
      RegisterOperations.registerOperations(
          key = key.get() ?: error("key is required to register operations"),
          graphID = graph.get() ?: error("graphID is required to register operations"),
          graphVariant = graphVariant.get() ?: error("graphVariant is required to register operations"),
          operationOutput = operationOutput.get().asFile.toOperationOutput()
      )
    }
  }
}