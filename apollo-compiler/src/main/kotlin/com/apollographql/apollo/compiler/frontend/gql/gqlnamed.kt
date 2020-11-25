package com.apollographql.apollo.compiler.frontend.gql

/**
 * Will return a Pair containing the filtered
 */
internal fun <T: GQLNamed> List<T>.partitionDuplicates(): Pair<List<T>, List<T>> {
  val result = mutableListOf<T>()
  val duplicates = mutableListOf<T>()

  var lastName:String? = null
  sortedBy { it.name }.forEach {
    if (it.name != lastName) {
      lastName = it.name
      result.add(it)
    }else {
      duplicates.add(it)
    }
  }

  return result to duplicates
}