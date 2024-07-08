package com.apollographql.apollo.tooling

import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.tooling.platformapi.public.PublishOperationsMutation
import com.apollographql.apollo.tooling.platformapi.public.type.OperationType
import com.apollographql.apollo.tooling.platformapi.public.type.PersistedQueryInput
import kotlinx.coroutines.runBlocking

class PersistedQuery(
    val name: String,
    val id: String,
    val body: String,
    val operationType: String,
)

sealed interface PublishOperationsResult
object GraphNotFound : PublishOperationsResult
class PermissionError(val message: String) : PublishOperationsResult
class CannotModifyOperationBody(val message: String) : PublishOperationsResult
class PublishOperationsSuccess(
    val added: Int,
    val removed: Int,
    val identical: Int,
    val updated: Int,
    val unaffected: Int,
    val name: String,
    val revision: Int,
) : PublishOperationsResult

@ApolloExperimental
fun publishOperations(
    listId: String,
    persistedQueries: List<PersistedQuery>,
    apolloKey: String,
    graph: String?,
): PublishOperationsResult {
  val graphID = graph ?: apolloKey.getGraph() ?: error("graph not found")

  val response = runBlocking {
    val mutation = PublishOperationsMutation(graphID, listId, Optional.present(
        persistedQueries.map { PersistedQueryInput(body = it.body, id = it.id, name = it.name, type = OperationType.safeValueOf(it.operationType.uppercase())) }
    ))
    apolloClient.mutation(mutation)
        .addHttpHeader("x-api-key", apolloKey)
        .execute()
  }

  val data = response.data
  if (data == null) {
    throw response.toException("Cannot publish operations")
  }

  val graph1 = data.graph
  if (graph1 == null) {
    return GraphNotFound
  }
  val ops = graph1.persistedQueryList.publishOperations
  return when {
    ops.onPublishOperationsResult != null -> {
      val counts = ops.onPublishOperationsResult.build.publish.operationCounts
      PublishOperationsSuccess(
          counts.added,
          counts.removed,
          counts.identical,
          counts.updated,
          counts.unaffected,
          ops.onPublishOperationsResult.build.list.name,
          ops.onPublishOperationsResult.build.revision
      )
    }

    ops.onPermissionError != null -> {
      PermissionError(ops.onPermissionError.message)
    }

    ops.onCannotModifyOperationBodyError != null -> {
      CannotModifyOperationBody(ops.onCannotModifyOperationBodyError.message)
    }

    else -> error("Unknown ops: ${ops.__typename}")
  }

}
