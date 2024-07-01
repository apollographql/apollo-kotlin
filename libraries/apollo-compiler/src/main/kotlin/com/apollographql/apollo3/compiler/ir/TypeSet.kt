package com.apollographql.apollo.compiler.ir

/**
 * A list of type conditions resulting from evaluating multiple potentially nested fragments.
 */
internal typealias TypeSet = Set<String>
/**
 * A list of concrete types, usually used with a a [TypeSet]
 */
internal typealias PossibleTypes = Set<String>

internal fun TypeSet.implements(other: TypeSet) = intersect(other) == other

internal fun subTypeCount(typeSet: TypeSet, candidates: Collection<TypeSet>): Int {
  return candidates.count { candidate ->
    candidate.implements(typeSet) && candidate != typeSet
  }
}

internal fun reduction(typeSets: Collection<TypeSet>): List<TypeSet> {
  return typeSets.filter { typeSet ->
    subTypeCount(typeSet, typeSets) == 0
  }
}

internal fun strictlySuperTypeSets(typeSet: TypeSet, candidates: Collection<TypeSet>): List<TypeSet> {
  return reduction(candidates.filter {
    typeSet.implements(it) && typeSet != it
  })
}

internal fun superTypeSets(typeSet: TypeSet, candidates: Collection<TypeSet>): List<TypeSet> {
  return reduction(candidates.filter {
    typeSet.implements(it)
  })
}

