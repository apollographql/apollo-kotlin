package com.apollographql.apollo3.cache.normalized.sql.internal

import com.apollographql.apollo3.cache.normalized.api.Record
import com.apollographql.apollo3.cache.normalized.api.RecordFieldJsonAdapter
import com.apollographql.apollo3.cache.normalized.sql.CacheQueries

internal object CacheQueriesHelpers {

  fun CacheQueries.selectRecord(key: String): Record? {
    return recordForKey(key)
        .executeAsList()
        .firstOrNull()
        ?.let {
          Record(
              key = it.key,
              fields = RecordFieldJsonAdapter.fromJson(it.record)!!,
          )
        }
  }

  fun CacheQueries.selectRecords(keys: Collection<String>): Collection<Record> {
    return keys.chunked(999).flatMap { chunkedKeys ->
      recordsForKeys(chunkedKeys)
          .executeAsList()
          .map {
            Record(
                key = it.key,
                fields = RecordFieldJsonAdapter.fromJson(it.record)!!,
            )
          }
    }
  }

  fun CacheQueries.updateRecords(records: Collection<Record>): Set<String> {
    var updatedRecordKeys: Set<String> = emptySet()
    transaction {
      val oldRecords = selectRecords(
          keys = records.map { it.key },
      ).associateBy { it.key }

      updatedRecordKeys = records.flatMap { record ->
        val oldRecord = oldRecords[record.key]
        if (oldRecord == null) {
          insert(
              key = record.key,
              record = RecordFieldJsonAdapter.toJson(record.fields),
          )
          record.fieldKeys()
        } else {
          val (mergedRecord, changedKeys) = oldRecord.mergeWith(record)
          if (mergedRecord.isNotEmpty()) {
            update(
                key = oldRecord.key,
                record = RecordFieldJsonAdapter.toJson(mergedRecord.fields),
            )
          }
          changedKeys
        }
      }.toSet()
    }
    return updatedRecordKeys
  }

  fun CacheQueries.updateRecord(record: Record): Set<String> {
    var updatedRecordKeys: Set<String> = emptySet()
    transaction {
      val oldRecord = selectRecord(record.key)

      updatedRecordKeys = if (oldRecord == null) {
        insert(
            key = record.key,
            record = RecordFieldJsonAdapter.toJson(record.fields),
        )
        record.fieldKeys()
      } else {
        val (mergedRecord, changedKeys) = oldRecord.mergeWith(record)
        if (mergedRecord.isNotEmpty()) {
          update(
              key = oldRecord.key,
              record = RecordFieldJsonAdapter.toJson(mergedRecord.fields),
          )
        }
        changedKeys
      }
    }
    return updatedRecordKeys
  }

  fun CacheQueries.remove(pattern: String): Int {
    var result = 0L
    transaction {
      deleteRecordsWithKeyMatching(pattern, "\\")
      result = changes().executeAsOne()
    }
    return result.toInt()
  }

  fun CacheQueries.deleteRecord(key: String, cascade: Boolean): Boolean {
    var result = false
    transaction {
      result = if (cascade) {
        selectRecord(key)
            ?.referencedFields()
            ?.all {
              deleteRecord(
                  key = it.key,
                  cascade = true,
              )
            }
            ?: false
      } else {
        delete(key)
        changes().executeAsOne() > 0
      }
    }
    return result
  }

  fun CacheQueries.deleteAllRecords() {
    transaction {
      deleteAll()
    }
  }

  fun CacheQueries.selectAllRecords(): Map<String, Record> {
    return selectRecords().executeAsList().map {
      it.key to Record(
          key = it.key,
          fields = RecordFieldJsonAdapter.fromJson(it.record)!!,
      )
    }.toMap()
  }
}
