package com.apollographql.apollo3.compiler.operationoutput

import kotlinx.serialization.Serializable

/**
 * [OperationOutput] is a map where the operationId is the key and [OperationDescriptor] the value
 *
 * By default, the operationId is a sha256. It can be changed for custom whitelisting/persisted queries implementations.
 */
typealias OperationOutput = Map<String, OperationDescriptor>

/**
 * A description of an operation to compute an id from
 */
@Serializable
class OperationDescriptor(
    /**
     * The name of the operation
     */
    val name: String,
    /**
     * The source of the operation document as it is sent over the wire, including fragments
     */
    val source: String,
    /**
     * The type of the operation. One of "query", "mutation", "subscription"
     */
    val type: String
)

/**
 * The id of an operation associated with its name so that it can be looked up.
 */
class OperationId(val id: String, val name: String)

internal fun OperationOutput.findOperationId(name: String): String {
  val id = entries.find { it.value.name == name }?.key
  check(id != null) {
    "cannot find operation ID for '$name', check your operationOutput.json"
  }
  return id
}

