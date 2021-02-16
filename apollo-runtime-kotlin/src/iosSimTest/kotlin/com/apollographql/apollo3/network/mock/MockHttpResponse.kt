package com.apollographql.apollo3.network.mock

import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSHTTPURLResponse

class MockHttpResponse(
    val httpData: NSData? = null,
    val httpResponse: NSHTTPURLResponse? = null,
    val error: NSError? = null
)
