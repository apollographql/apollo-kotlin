@file:Suppress("ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT")

package com.apollographql.apollo3.adapter

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import java.math.BigDecimal as JBigDecimal

@Deprecated("BigDecimal has new maven coordinates at 'com.apollographql.adapters:apollo-adapters-core. See https://go.apollo.dev/ak-4-migration-guide for more details.")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
actual typealias BigDecimal = JBigDecimal

@Suppress("DEPRECATION")
actual fun BigDecimal.toNumber(): Number = this
