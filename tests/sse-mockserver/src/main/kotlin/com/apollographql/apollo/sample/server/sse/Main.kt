package com.apollographql.apollo.sample.server.sse

import au.com.woolworths.sample.graphqlsse.server.KtorServerInteractor

fun main(args: Array<String>) {
  
  KtorServerInteractor()
      .invoke()

}

