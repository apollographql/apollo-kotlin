package com.apollographql.apollo.compiler

import kotlinx.serialization.Serializable

/**
 * The complete set of fragment names transitively referenced by operations in the configured downstream
 * modules, as computed by [com.apollographql.apollo.compiler.ir.computeUsedFragmentNames].
 *
 * This mirrors [UsedCoordinates]: it is computed per-module and merged transitively across the downstream
 * dependency graph so that a fragment defined in an upstream/schema module is never reported as unused as long
 * as any module downstream of it (however many `dependsOn(..., bidirectional = true)` hops away) spreads it.
 */
@Serializable
class UsedFragmentNames(private val names: Set<String> = emptySet()) {
  fun asSet(): Set<String> = names

  fun mergeWith(other: UsedFragmentNames): UsedFragmentNames {
    return UsedFragmentNames(names + other.names)
  }
}

fun Set<String>.toUsedFragmentNames(): UsedFragmentNames = UsedFragmentNames(this)
