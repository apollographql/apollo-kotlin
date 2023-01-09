package com.example

import com.apollographql.apollo3.api.OperationName

suspend fun main() {
  val OPERATION_NAME: OperationName = object : OperationName {
    override fun name(): String = "MyMutation1"
  }
  println(OPERATION_NAME)
}
