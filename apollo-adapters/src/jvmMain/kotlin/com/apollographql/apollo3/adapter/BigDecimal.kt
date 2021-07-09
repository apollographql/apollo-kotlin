package com.apollographql.apollo3.api

import java.math.BigDecimal as JBigDecimal

actual typealias BigDecimal = JBigDecimal

actual fun BigDecimal.toNumber(): Number = this
