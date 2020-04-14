package com.apollographql.apollo.cache.normalized

class RecordSet {
  private val recordMap = mutableMapOf<String, Record>()

  operator fun get(key: String): Record? = recordMap[key]

  fun merge(apolloRecord: Record): Set<String> {
    val oldRecord = recordMap[apolloRecord.key]
    return if (oldRecord == null) {
      recordMap[apolloRecord.key] = apolloRecord
      emptySet()
    } else {
      oldRecord.mergeWith(apolloRecord)
    }
  }

  fun allRecords(): Collection<Record> = recordMap.values.toList()
}
