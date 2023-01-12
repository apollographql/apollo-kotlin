package com.example

import com.apollographql.apollo.api.OperationName

suspend fun main() {
  val OPERATION_NAME: OperationName = object : OperationName {
    override fun name(): String = "MyMutation1"
  }
  println(OPERATION_NAME.name())
}
