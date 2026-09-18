package com.apollographql.apollo.compiler

import kotlinx.serialization.Serializable

/**
 * The set of fragment names that are known to be spread from *somewhere* in a multi-module project: either
 * directly from an operation/fragment in the current module, or from a downstream module's operations/fragments
 * (see [com.apollographql.apollo.compiler.ir.IrOperations.reachableFragmentNames]).
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
