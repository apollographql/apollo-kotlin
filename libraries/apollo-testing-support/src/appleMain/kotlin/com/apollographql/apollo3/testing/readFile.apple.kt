package com.apollographql.apollo.testing

import com.apollographql.apollo.annotations.ApolloDeprecatedSince

@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
@Deprecated("This is only used for internal Apollo tests and will be removed in a future version.")
actual fun shouldUpdateTestFixtures(): Boolean = false

@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
@Deprecated("This is only used for internal Apollo tests and will be removed in a future version.")
actual val testsPath: String = "../"
