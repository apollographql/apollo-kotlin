package com.apollographql.apollo.exception

import platform.Foundation.NSError

val ApolloNetworkException.nsError
  get() = platformCause as? NSError