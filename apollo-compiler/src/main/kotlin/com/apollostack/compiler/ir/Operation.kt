package com.apollostack.compiler.ir

class Operation(val operationName: String, val operationType: String, val variables: List<String>,
    val source: String, val fields: List<Field>) {

}