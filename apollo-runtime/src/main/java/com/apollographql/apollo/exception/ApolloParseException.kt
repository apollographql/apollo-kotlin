package com.apollographql.apollo.exception

class ApolloParseException : ApolloException {
  constructor(message: String?) : super(message!!) {}
  constructor(message: String?, cause: Throwable?) : super(message!!, cause!!) {}
}