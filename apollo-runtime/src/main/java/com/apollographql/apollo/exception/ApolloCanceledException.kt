package com.apollographql.apollo.exception

class ApolloCanceledException : ApolloException {
  constructor() : super("Call is cancelled") {}
  constructor(message: String?, cause: Throwable?) : super(message!!, cause!!) {}
}