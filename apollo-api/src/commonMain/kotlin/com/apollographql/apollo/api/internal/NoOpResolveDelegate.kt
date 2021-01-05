package com.apollographql.apollo.api.internal

import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.ResponseField

class NoOpResolveDelegate<T>: ResolveDelegate<T> {
  override fun willResolveRootQuery(operation: Operation<*>) {
  }

  override fun willResolve(field: ResponseField, variables: Operation.Variables, value: Any?) {
  }

  override fun didResolve(field: ResponseField, variables: Operation.Variables) {
  }

  override fun didResolveScalar(value: Any?) {
  }

  override fun willResolveObject(objectField: ResponseField, objectSource: T?) {
  }

  override fun didResolveObject(objectField: ResponseField, objectSource: T?) {
  }

  override fun didResolveList(array: List<*>) {
  }

  override fun willResolveElement(atIndex: Int) {
  }

  override fun didResolveElement(atIndex: Int) {
  }

  override fun didResolveNull() {
  }
}