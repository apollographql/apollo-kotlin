package com.apollographql.apollo.gradle.task

import com.apollographql.apollo.compiler.MANIFEST_OPERATION_OUTPUT
import com.apollographql.apollo.compiler.MANIFEST_PERSISTED_QUERY
import com.apollographql.apollo.compiler.toOperationOutput
import com.apollographql.apollo.compiler.toPersistedQueryManifest
import com.apollographql.apollo.tooling.CannotModifyOperationBody
import com.apollographql.apollo.tooling.GraphNotFound
import com.apollographql.apollo.tooling.PermissionError
import com.apollographql.apollo.tooling.PersistedQuery
import com.apollographql.apollo.tooling.PublishOperationsSuccess
import com.apollographql.apollo.tooling.RegisterOperations
import com.apollographql.apollo.tooling.publishOperations
import gratatouille.tasks.GInputFile
import gratatouille.tasks.GLogger
import gratatouille.tasks.GTask

@GTask(pure = false)
internal fun apolloRegisterOperations(
    logger: GLogger,
    operationOutput: GInputFile,
    operationManifestFormat: String?,
    listId: String?,
    key: String?,
    graph: String?,
    graphVariant: String?,
) {
  if (listId != null) {
    check(operationManifestFormat == MANIFEST_PERSISTED_QUERY) {
      """Apollo: registering operations to a persisted query list requires operationManifestFormat = "$MANIFEST_PERSISTED_QUERY":
          |apollo {
          |  service("service") {
          |    operationManifestFormat.set("$MANIFEST_PERSISTED_QUERY")
          |  }
          |}
        """.trimMargin()
    }
    val result = publishOperations(
        listId = listId,
        persistedQueries = operationOutput.toPersistedQueryManifest().operations.map {
          PersistedQuery(
              name = it.name,
              id = it.id,
              body = it.body,
              operationType = it.type
          )
        },
        apolloKey = key ?: error("key is required to register operations"),
        graph = graph
    )

    when(result) {
      is PublishOperationsSuccess -> {
        logger.lifecycle("Apollo: persisted query list uploaded successfully")
      }

      is CannotModifyOperationBody -> error("Cannot upload persisted query list: cannot modify operation body ('${result.message}')")
      GraphNotFound ->  error("Cannot upload persisted query list: graph '$graph' not found")
      is PermissionError -> error("Cannot upload persisted query list: permission error ('${result.message}')")
    }
  } else {
    logger.warn("Apollo: registering operations without a listId is deprecated")
    @Suppress("DEPRECATION_ERROR")
    check(operationManifestFormat == MANIFEST_OPERATION_OUTPUT) {
      """Apollo: registering legacy operations requires operationManifestFormat = "$MANIFEST_OPERATION_OUTPUT":
          |apollo {
          |  service("service") {
          |    operationManifestFormat.set("$MANIFEST_OPERATION_OUTPUT")
          |  }
          |}
        """.trimMargin()
    }
    @Suppress("DEPRECATION")
    RegisterOperations.registerOperations(
        key = key?: error("key is required to register operations"),
        graphID = graph ?: error("graphID is required to register operations"),
        graphVariant = graphVariant ?: error("graphVariant is required to register operations"),
        operationOutput = operationOutput.toOperationOutput()
    )
  }
}
