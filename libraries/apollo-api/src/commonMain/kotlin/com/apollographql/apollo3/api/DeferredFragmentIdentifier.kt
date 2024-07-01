package com.apollographql.apollo.api

data class DeferredFragmentIdentifier(
    /**
     * Path of the fragment in the overall JSON response. The elements can either be Strings (names) or Integers (array indices).
     */
    val path: List<Any>,
    val label: String?,
)
