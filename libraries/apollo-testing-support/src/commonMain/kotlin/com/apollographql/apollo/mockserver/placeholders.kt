package com.apollographql.apollo.mockserver

import com.apollographql.apollo.annotations.ApolloExperimental

/**
 * Redirect placeholders.
 * They are marked ApolloExperimental so that we don't track them in the public API
 */

@Deprecated(
    message = "MockServer has moved to new coordinates. See https://go.apollo.dev/ak-moved-artifacts/",
    level = DeprecationLevel.ERROR
)
@ApolloExperimental
class MockServer

@Deprecated(
    message = "MockServer has moved to new coordinates. See https://go.apollo.dev/ak-moved-artifacts/",
    level = DeprecationLevel.ERROR
)
@ApolloExperimental
class WebsocketMockRequest
