package com.apollographql.apollo3.network.http

import platform.Foundation.NSURL
import platform.Foundation.NSURLComponents
import platform.Foundation.NSURLQueryItem

actual class HttpUrlBuilder actual constructor(val baseUrl: String, val queryParameters: Map<String, String>) {

  actual fun build(): String {
    val urlComponents = NSURLComponents(uRL = NSURL(string = baseUrl), resolvingAgainstBaseURL = false)
    urlComponents.queryItems = queryParameters.map {
      NSURLQueryItem(name = it.key, value = it.value)
    }

    return urlComponents.URL!!.absoluteString!!
  }
}