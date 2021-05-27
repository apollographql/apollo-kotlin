package com.apollographql.apollo.internal.batch

import com.apollographql.apollo.interceptor.ApolloInterceptor

/**
 * Wrapper class holding a InterceptorRequest and its corresponding Callback
 */
data class QueryToBatch(
    val request: ApolloInterceptor.InterceptorRequest,
    val callback: ApolloInterceptor.CallBack
)