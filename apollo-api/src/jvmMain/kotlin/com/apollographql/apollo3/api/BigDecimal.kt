package com.apollographql.apollo.api

import java.math.BigDecimal as JBigDecimal

actual typealias BigDecimal = JBigDecimal

actual fun BigDecimal.toNumber(): Number = this
