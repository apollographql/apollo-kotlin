package com.apollostack.compiler.ir

class Operation(val operationName: String, val operationType: String, val variables: List<Variable>,
    val source: String, val fields: List<Field>) {

}