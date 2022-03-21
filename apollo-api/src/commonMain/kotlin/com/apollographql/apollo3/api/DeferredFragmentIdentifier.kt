package com.apollographql.apollo3.api

data class DeferredFragmentIdentifier(
    val path: String,
    val label: String?,
) {
  constructor(path: List<Any>, label: String?) : this(path.joinToString("."), label)
}
