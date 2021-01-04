package com.apollographql.apollo.exception

import okhttp3.Response

class ApolloHttpException(val rawResponse: Response?) : ApolloException(formatMessage(rawResponse)) {
  private val code: Int = rawResponse?.code() ?: 0

  fun code(): Int {
    return code
  }


  fun rawResponse(): Response? {
    return rawResponse
  }

  companion object {
    private fun formatMessage(response: Response?): String {
      return if (response == null) {
        "Empty HTTP response"
      } else "HTTP " + response.code() + " " + response.message()
    }
  }
}