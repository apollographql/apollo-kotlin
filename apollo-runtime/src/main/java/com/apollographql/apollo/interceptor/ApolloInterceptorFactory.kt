package com.apollographql.apollo.interceptor

interface ApolloInterceptorFactory {
  fun newInterceptor(): ApolloInterceptor
}