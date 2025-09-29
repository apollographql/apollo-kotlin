package com.apollographql.apollo.api

import com.apollographql.apollo.annotations.ApolloInternal

data class DeferredFragmentIdentifier(
    /**
     * Path of the fragment in the overall JSON response. The elements can either be Strings (names) or Integers (array indices).
     */
    val path: List<Any>,
    val label: String?,
) {
  companion object {
    /**
     * Special identifier to signal that the identifiers are pending, as in the modern version of the protocol.
     */
    @ApolloInternal
    val Pending = DeferredFragmentIdentifier(emptyList(), "__pending")
  }
}
