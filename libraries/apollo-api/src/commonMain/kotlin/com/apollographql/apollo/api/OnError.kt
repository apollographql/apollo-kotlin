package com.apollographql.apollo.api

import com.apollographql.apollo.annotations.ApolloExperimental

@ApolloExperimental
enum class OnError {
  NULL,
  PROPAGATE,
  HALT
}
