package com.apollographql.apollo3.compiler.pqm

import com.apollographql.apollo3.ast.QueryDocumentMinifier
import com.apollographql.apollo3.compiler.ir.DefaultIrOperations
import com.apollographql.apollo3.compiler.ir.IrOperations
import kotlinx.serialization.Serializable

@Serializable
class PersistedQueryManifest(
    val format: String,
    val version: Int,
    val operations: List<PqmOperation>
)

@Serializable
class PqmOperation(
    val id: String,
    val body: String,
    val name: String,
    val type: String,
)


internal fun IrOperations.toPersistedQueryManifest(): PersistedQueryManifest {
  this as DefaultIrOperations
  return PersistedQueryManifest(
      format = "apollo-persisted-query-manifest",
      version = 1,
      operations = operations.map {
        PqmOperation(
            id = it.id,
            body = QueryDocumentMinifier.minify(it.sourceWithFragments),
            name = it.name,
            type = it.operationType.name.lowercase()
        )
      }
  )
}