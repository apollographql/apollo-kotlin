package com.apollographql.apollo3.exception

import platform.Foundation.NSError

val ApolloNetworkException.nsError
  get() = platformCause as? NSError