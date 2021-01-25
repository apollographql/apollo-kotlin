package com.apollographql.apollo.cache.normalized.sql.internal

import com.apollographql.apollo.cache.normalized.Record
import com.apollographql.apollo.cache.normalized.RecordFieldJsonAdapter
import com.apollographql.apollo.cache.normalized.sql.ApolloDatabase

internal object RecordsetDataSource {

  fun ApolloDatabase.selectRecord(key: String, recordFieldAdapter: RecordFieldJsonAdapter): Record? {
    return cacheQueries.recordForKey(key)
        .executeAsList()
        .firstOrNull()
        ?.let {
          Record.builder(it.key)
              .addFields(recordFieldAdapter.from(it.record)!!)
              .build()
        }
  }

  fun ApolloDatabase.selectRecords(keys: Collection<String>, recordFieldAdapter: RecordFieldJsonAdapter): Collection<Record> {
    return keys.chunked(999).flatMap { chunkedKeys ->
      cacheQueries.recordsForKeys(chunkedKeys)
          .executeAsList()
          .map {
            Record.builder(it.key)
                .addFields(recordFieldAdapter.from(it.record)!!)
                .build()
          }
    }
  }

  fun ApolloDatabase.updateRecords(records: Collection<Record>, recordFieldAdapter: RecordFieldJsonAdapter): Set<String> {
    var updatedRecordKeys: Set<String> = emptySet()
    cacheQueries.transaction {
      val oldRecords = selectRecords(
          keys = records.map { it.key },
          recordFieldAdapter = recordFieldAdapter,
      ).associateBy { it.key }
      updatedRecordKeys = records.flatMap { record ->
        val oldRecord = oldRecords[record.key]
        if (oldRecord == null) {
          cacheQueries.insert(
              key = record.key,
              record = recordFieldAdapter.toJson(record.fields),
          )
          record.keys()
        } else {
          oldRecord.mergeWith(record).also {
            if (it.isNotEmpty()) {
              cacheQueries.update(
                  key = oldRecord.key,
                  record = recordFieldAdapter.toJson(oldRecord.fields),
              )
            }
          }
        }
      }.toSet()
    }
    return updatedRecordKeys
  }

  fun ApolloDatabase.deleteRecord(key: String, cascade: Boolean, recordFieldAdapter: RecordFieldJsonAdapter): Boolean {
    var result = false
    cacheQueries.transaction {
      result = if (cascade) {
        selectRecord(
            key = key,
            recordFieldAdapter = recordFieldAdapter,
        )
            ?.referencedFields()
            ?.all {
              deleteRecord(
                  key = it.key,
                  cascade = true,
                  recordFieldAdapter = recordFieldAdapter,
              )
            }
            ?: false
      } else {
        cacheQueries.delete(key)
        cacheQueries.changes().executeAsOne() > 0
      }
    }
    return result
  }

  fun ApolloDatabase.deleteAllRecords() {
    cacheQueries.transaction {
      cacheQueries.deleteAll()
    }
  }

  fun ApolloDatabase.selectAllRecords(recordFieldAdapter: RecordFieldJsonAdapter): Map<String, Record> {
    return cacheQueries.selectRecords().executeAsList().map {
      it.key to Record.builder(it.key)
          .addFields(recordFieldAdapter.from(it.record)!!)
          .build()
    }.toMap()
  }
}
