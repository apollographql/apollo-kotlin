package com.apollographql.apollo3.api

data class DeferredFragmentIdentifier(
    /**
     * Values in the list can be either String or Int.
     */
    val path: List<Any>,
    val label: String?,
)
