package com.apollographql.apollo3.api.internal

import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.ResponseField

interface ResolveDelegate<R> {
  fun willResolveRootQuery(operation: Operation<*>)
  fun willResolve(field: ResponseField, variables: Operation.Variables, value: Any?)
  fun didResolve(field: ResponseField, variables: Operation.Variables)
  fun didResolveScalar(value: Any?)
  fun willResolveObject(objectField: ResponseField, objectSource: R?)
  fun didResolveObject(objectField: ResponseField, objectSource: R?)
  fun didResolveList(array: List<*>)
  fun willResolveElement(atIndex: Int)
  fun didResolveElement(atIndex: Int)
  fun didResolveNull()
}
