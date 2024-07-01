package com.apollographql.apollo.cache.normalized.sql.internal

import com.apollographql.apollo.cache.normalized.api.Record
import com.apollographql.apollo.cache.normalized.api.internal.JsonRecordSerializer
import com.apollographql.apollo.cache.normalized.sql.internal.json.JsonQueries

internal class JsonRecordDatabase(private val jsonQueries: JsonQueries): RecordDatabase {
  override fun select(key: String): Record? {
    return jsonQueries.recordForKey(key).executeAsList()
        .map {
          JsonRecordSerializer.deserialize(it.key, it.record)
        }
        .singleOrNull()
  }

  override fun select(keys: Collection<String>): List<Record> {
    return jsonQueries.recordsForKeys(keys).executeAsList()
        .map {
          JsonRecordSerializer.deserialize(it.key, it.record)
        }
  }

  override fun <T> transaction(noEnclosing: Boolean, body: () -> T): T {
    return jsonQueries.transactionWithResult {
      body()
    }
  }

  override fun delete(key: String) {
    jsonQueries.delete(key)
  }

  override fun deleteMatching(pattern: String) {
    jsonQueries.deleteRecordsWithKeyMatching(pattern, "\\")
  }

  override fun deleteAll() {
    jsonQueries.deleteAll()
  }

  override fun changes(): Long {
    return jsonQueries.changes().executeAsOne()
  }

  override fun insert(record: Record) {
    jsonQueries.insert(record.key, JsonRecordSerializer.serialize(record))
  }

  override fun update(record: Record) {
    jsonQueries.update(JsonRecordSerializer.serialize(record), record.key)
  }

  override fun selectAll(): List<Record> {
    return jsonQueries.selectRecords().executeAsList().map {
      JsonRecordSerializer.deserialize(it.key, it.record)
    }
  }
}