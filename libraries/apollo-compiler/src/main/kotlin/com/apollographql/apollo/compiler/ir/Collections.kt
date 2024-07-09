package com.apollographql.apollo.compiler.ir

import java.math.BigInteger

/**
 * Helper function to compute the intersection of multiple Sets
 */
internal fun <T> Collection<Set<T>>.intersection(): Set<T> {
  if (isEmpty()) {
    return emptySet()
  }

  return drop(1).fold(first()) { acc, list ->
    acc.intersect(list)
  }
}

/**
 * Helper function to compute the intersection of multiple Sets
 */
internal fun <T> Collection<Set<T>>.union(): Set<T> {
  return fold(emptySet()) { acc, list ->
    acc.union(list)
  }
}

internal fun <T> Collection<T>.pairs(): List<Pair<T, T>> {
  val result = mutableListOf<Pair<T, T>>()
  val asList = toList()
  for (i in 0.until(size)) {
    for (j in (i + 1).until(size)) {
      result.add(asList[i] to asList[j])
    }
  }
  return result
}

/**
 * For a list of items, returns all possible combinations as in https://en.wikipedia.org/wiki/Combination
 *
 * Not an extension function so as not to pollute the List<T> namespace
 */
internal fun <T> List<T>.combinations(includeEmpty: Boolean = false): List<List<T>> {
  val start = if (includeEmpty) 0 else 1
  val end = BigInteger.valueOf(2).pow(size).intValueExact()

  return start.until(end).fold(emptyList()) { acc, bitmask ->
    acc + listOf(
        0.until(size).mapNotNull { position ->
          if (bitmask.and(1.shl(position)) != 0) {
            get(position)
          } else {
            null
          }
        }
    )
  }
}
