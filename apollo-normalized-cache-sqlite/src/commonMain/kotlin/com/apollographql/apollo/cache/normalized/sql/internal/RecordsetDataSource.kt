package com.apollographql.apollo.cache.normalized.sql.internal

import com.apollographql.apollo.cache.normalized.Record
import com.apollographql.apollo.cache.normalized.RecordFieldJsonAdapter
import com.apollographql.apollo.cache.normalized.sql.CacheQueries

internal object RecordsetDataSource {

  fun CacheQueries.selectRecord(key: String): Record? {
    return recordForKey(key)
        .executeAsList()
        .firstOrNull()
        ?.let {
          Record.builder(it.key)
              .addFields(RecordFieldJsonAdapter.fromJson(it.record)!!)
              .build()
        }
  }

  fun CacheQueries.selectRecords(keys: Collection<String>): Collection<Record> {
    return keys.chunked(999).flatMap { chunkedKeys ->
      recordsForKeys(chunkedKeys)
          .executeAsList()
          .map {
            Record.builder(it.key)
                .addFields(RecordFieldJsonAdapter.fromJson(it.record)!!)
                .build()
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
          record.keys()
        } else {
          oldRecord.mergeWith(record).also {
            if (it.isNotEmpty()) {
              update(
                  key = oldRecord.key,
                  record = RecordFieldJsonAdapter.toJson(oldRecord.fields),
              )
            }
          }
        }
      }.toSet()
    }
    return updatedRecordKeys
  }

  fun CacheQueries.updateRecord(record: Record): Set<String> {
    var updatedRecordKeys: Set<String> = emptySet()
    transaction {
      val oldRecord = selectRecord(record.key)

      if (oldRecord == null) {
        insert(
            key = record.key,
            record = RecordFieldJsonAdapter.toJson(record.fields),
        )
        updatedRecordKeys = record.keys()
      } else {
        updatedRecordKeys = oldRecord.mergeWith(record).also {
          if (it.isNotEmpty()) {
            update(
                key = oldRecord.key,
                record = RecordFieldJsonAdapter.toJson(oldRecord.fields),
            )
          }
        }
      }
    }
    return updatedRecordKeys
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
      it.key to Record.builder(it.key)
          .addFields(RecordFieldJsonAdapter.fromJson(it.record)!!)
          .build()
    }.toMap()
  }
}
