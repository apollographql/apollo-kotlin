package com.apollographql.apollo.exception

import okhttp3.Response

class ApolloHttpException(rawResponse: Response?) : ApolloException(formatMessage(rawResponse)) {
  private val code: Int
  override val message: String

  @Transient
  private val rawResponse: Response?
  fun code(): Int {
    return code
  }

  fun message(): String {
    return message
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

  init {
    code = rawResponse?.code() ?: 0
    message = if (rawResponse != null) rawResponse.message() else ""
    this.rawResponse = rawResponse
  }
}