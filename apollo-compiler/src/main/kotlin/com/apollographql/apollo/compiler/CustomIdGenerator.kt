package com.apollographql.apollo.compiler

interface CustomIdGenerator {
  fun apply(queryString: String): String
}
