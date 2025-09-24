package com.apollographql.apollo.api

import com.apollographql.apollo.annotations.ApolloInternal

data class DeferredFragmentIdentifier(
    /**
     * Path of the fragment in the overall JSON response. The elements can either be Strings (names) or Integers (array indices).
     */
    val path: List<Any>,
    val label: String?,
) {
  internal companion object {
    /**
     * Special identifier to signal that the identifiers are pending, as in the modern version of the protocol.
     */
    internal val Pending = DeferredFragmentIdentifier(emptyList(), "__pending")
  }
}

/**
 * Identifies an incremental result.
 * [DeferredFragmentIdentifier] is kept to preserve the API/ABI, but this alias is more descriptive of its purpose.
 */
typealias IncrementalResultIdentifier = DeferredFragmentIdentifier

typealias IncrementalResultIdentifiers = Set<IncrementalResultIdentifier>

@ApolloInternal
fun IncrementalResultIdentifiers.isPending(): Boolean {
  return any { it === DeferredFragmentIdentifier.Pending }
}

@ApolloInternal
fun IncrementalResultIdentifiers.pending(): IncrementalResultIdentifiers {
  return this + DeferredFragmentIdentifier.Pending
}

@ApolloInternal
fun IncrementalResultIdentifiers.nonPending(): IncrementalResultIdentifiers {
  return filter { it !== DeferredFragmentIdentifier.Pending }.toSet()
}
