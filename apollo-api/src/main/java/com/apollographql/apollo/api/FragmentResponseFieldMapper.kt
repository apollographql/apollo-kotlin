package com.apollographql.apollo.api

import java.io.IOException

/**
 * FragmentResponseFieldMapper is responsible for mapping the response back to a fragment of type T.
 */
interface FragmentResponseFieldMapper<T> {
  @Throws(IOException::class)
  fun map(responseReader: ResponseReader, conditionalType: String): T
}
