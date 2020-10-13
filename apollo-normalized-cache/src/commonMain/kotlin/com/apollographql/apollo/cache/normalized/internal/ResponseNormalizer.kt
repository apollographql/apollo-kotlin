package com.apollographql.apollo.cache.normalized.internal

import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.ResolveDelegate
import com.apollographql.apollo.cache.normalized.CacheKey
import com.apollographql.apollo.cache.normalized.CacheKeyResolver.Companion.rootKeyForOperation
import com.apollographql.apollo.cache.normalized.CacheReference
import com.apollographql.apollo.cache.normalized.Record
import com.apollographql.apollo.cache.normalized.Record.Companion.builder
import com.apollographql.apollo.cache.normalized.RecordSet
import kotlin.jvm.JvmField

abstract class ResponseNormalizer<R> : ResolveDelegate<R> {
  private lateinit var pathStack: SimpleStack<MutableList<String>>
  private lateinit var recordStack: SimpleStack<Record>
  private lateinit  var valueStack: SimpleStack<Any?>
  private lateinit var path: MutableList<String>
  private lateinit var currentRecordBuilder: Record.Builder
  private var recordSet = RecordSet()
  private var dependentKeys = mutableSetOf<String>()

  open fun records(): Collection<Record?>? {
    return recordSet.allRecords()
  }

  open fun dependentKeys(): Set<String> {
    return dependentKeys
  }

  override fun willResolveRootQuery(operation: Operation<*, *>) {
    willResolveRecord(rootKeyForOperation(operation))
  }

  override fun willResolve(field: ResponseField, variables: Operation.Variables, value: Any?) {
    val key = cacheKeyBuilder().build(field, variables)
    path.add(key)
  }

  override fun didResolve(field: ResponseField, variables: Operation.Variables) {
    path.removeAt(path.size - 1)
    val value = valueStack.pop()
    val cacheKey = cacheKeyBuilder().build(field, variables)
    val dependentKey = currentRecordBuilder.key + "." + cacheKey
    dependentKeys.add(dependentKey)
    currentRecordBuilder.addField(cacheKey, value)
    if (recordStack.isEmpty) {
      recordSet.merge(currentRecordBuilder.build())
    }
  }

  override fun didResolveScalar(value: Any?) {
    valueStack.push(value)
  }

  override fun willResolveObject(objectField: ResponseField, objectSource: R?) {
    pathStack.push(path)
    val cacheKey = objectSource?.let { resolveCacheKey(objectField, it) } ?: CacheKey.NO_KEY
    var cacheKeyValue = cacheKey.key
    if (cacheKey.equals(CacheKey.NO_KEY)) {
      cacheKeyValue = pathToString()
    } else {
      path = ArrayList()
      path.add(cacheKeyValue)
    }
    recordStack.push(currentRecordBuilder.build())
    currentRecordBuilder = builder(cacheKeyValue)
  }

  override fun didResolveObject(objectField: ResponseField, objectSource: R?) {
    path = pathStack.pop()
    if (objectSource != null) {
      val completedRecord = currentRecordBuilder.build()
      valueStack.push(CacheReference(completedRecord.key))
      dependentKeys.add(completedRecord.key)
      recordSet.merge(completedRecord)
    }
    currentRecordBuilder = recordStack.pop().toBuilder()
  }

  override fun didResolveList(array: List<*>) {
    val parsedArray = ArrayList<Any?>(array.size)
    var i = 0
    val size = array.size
    while (i < size) {
      parsedArray.add(0, valueStack.pop())
      i++
    }
    valueStack.push(parsedArray)
  }

  override fun willResolveElement(atIndex: Int) {
    path.add(atIndex.toString())
  }

  override fun didResolveElement(atIndex: Int) {
    path.removeAt(path.size - 1)
  }

  override fun didResolveNull() {
    valueStack.push(null)
  }

  abstract fun resolveCacheKey(field: ResponseField, record: R): CacheKey
  abstract fun cacheKeyBuilder(): CacheKeyBuilder
  fun willResolveRecord(cacheKey: CacheKey) {
    pathStack = SimpleStack()
    recordStack = SimpleStack()
    valueStack = SimpleStack()
    dependentKeys = HashSet()
    path = ArrayList()
    currentRecordBuilder = builder(cacheKey.key)
    recordSet = RecordSet()
  }

  private fun pathToString(): String {
    val stringBuilder = StringBuilder()
    var i = 0
    val size = path.size
    while (i < size) {
      val pathPiece = path[i]
      stringBuilder.append(pathPiece)
      if (i < size - 1) {
        stringBuilder.append(".")
      }
      i++
    }
    return stringBuilder.toString()
  }

  companion object {
    @JvmField
    val NO_OP_NORMALIZER: ResponseNormalizer<*> = object : ResponseNormalizer<Any?>() {
      override fun willResolveRootQuery(operation: Operation<*, *>) {}
      override fun willResolve(field: ResponseField, variables: Operation.Variables, value: Any?) {}
      override fun didResolve(field: ResponseField, variables: Operation.Variables) {}
      override fun didResolveScalar(value: Any?) {}
      override fun willResolveObject(objectField: ResponseField, objectSource: Any?) {}
      override fun didResolveObject(objectField: ResponseField, objectSource: Any?) {}
      override fun didResolveList(array: List<*>) {}
      override fun willResolveElement(atIndex: Int) {}
      override fun didResolveElement(atIndex: Int) {}
      override fun didResolveNull() {}
      override fun records(): Collection<Record?>? {
        return emptyList()
      }

      override fun dependentKeys(): Set<String> {
        return emptySet()
      }

      override fun resolveCacheKey(field: ResponseField, record: Any?): CacheKey {
        return CacheKey.NO_KEY
      }

      override fun cacheKeyBuilder(): CacheKeyBuilder {
        return object : CacheKeyBuilder {
          override fun build(field: ResponseField, variables: Operation.Variables): String {
            return CacheKey.NO_KEY.key
          }
        }
      }
    }
  }
}
