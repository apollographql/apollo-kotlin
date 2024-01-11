package com.apollographql.apollo3.compiler

typealias UsedCoordinates = Map<String, Set<String>>

fun UsedCoordinates.mergeWith(other: UsedCoordinates): UsedCoordinates {
  return (entries + other.entries).groupBy { it.key }.mapValues {
    it.value.map { it.value }.fold(emptySet()) { acc, set ->
      (acc + set).toSet()
    }
  }
}
